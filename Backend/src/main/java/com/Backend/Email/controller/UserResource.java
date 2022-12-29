package com.Backend.Email.controller;

import com.Backend.Email.model.contact.Contact;
import com.Backend.Email.model.email.Email;
import com.Backend.Email.model.email.EmailBuilder;
import com.Backend.Email.model.user.User;
import com.Backend.Email.services.ContactService;
import com.Backend.Email.services.EmailService;
import com.Backend.Email.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@CrossOrigin
@RequestMapping("/")
@RestController
public class UserResource {
    private final UserService userService;
    private final EmailService emailService;

    private final ContactService contactService;

    public UserResource(UserService userService, EmailService emailService, ContactService contactService){
        this.userService = userService;
        this.emailService = emailService;
        this.contactService = contactService;
    }

    @GetMapping("/user/getAll")
    public ResponseEntity<List<User>>  getAllUsers(){
        List<User> users =  userService.findAllUsers();
        System.out.println(users.toString());
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/user/find/{email}")
    public ResponseEntity<User>  getUserById(@PathVariable("email") String email){
        User user =  userService.findUser(email);
        System.out.println(email.toString());
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PostMapping("/user/add")
    public ResponseEntity<User> addUser(@RequestBody User user){
        User newUser = userService.saveUser(user);
        return new ResponseEntity<>(newUser, HttpStatus.CREATED);
    }

    @PutMapping("/user/update")
    public ResponseEntity<User> updateUser(@RequestBody User user){
        User updateUser = userService.saveUser(user);
        return new ResponseEntity<>(updateUser, HttpStatus.OK);
    }


    @DeleteMapping("/user/delete/{email}")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable("email") String email){
        userService.deleteUser(email);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/email/compose/{draft}")
    public ResponseEntity sendEmail(@RequestBody Object finishedEmail, @PathVariable boolean draft) throws IOException {

        Map<String, Object> res = new ObjectMapper().convertValue(finishedEmail, HashMap.class);
        EmailBuilder emailBuilder = new EmailBuilder();

        boolean finished = true;

        finished = emailBuilder.setFrom(res.get("from")) && finished;
        finished = emailBuilder.setTo((res.get("to"))) && finished;
        finished = emailBuilder.setSubject(res.get("subject")) && finished;
        finished = emailBuilder.setBody(res.get("body")) && finished;
        emailBuilder.setPriority(res.get("priority"));
        emailBuilder.setDate(LocalDateTime.now());
        emailBuilder.setId(res.get("id"));
//        emailBuilder.setAttachments(res.get("attachments"));

        User user = userService.findUser(res.get("from").toString());
        Email email = emailService.addEmail(emailBuilder.getEmail());

        if(finished && !draft) {
            List<Integer> notExist = null;

            if (user != null) {
                user.sendEmail(userService, email);
            }
        }else{
            user.addToDraft(email.getId(), userService);
            email.setLinks(1);
        }

        emailService.addEmail(email);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }


    @GetMapping("/email/getEmails/{email}/{folderName}/{ids}")
    public ResponseEntity<List<Email>>  getEmails(@PathVariable List<Long> ids, @PathVariable String folderName, @PathVariable String email){
        List<Email> emails = emailService.findEmails(ids);
        if(folderName.equals("trash")){
            LocalDateTime currDateTime = LocalDateTime.now().minusDays(30);
            User user = userService.findUser(email);
            List<LocalDateTime> deletionTimes = user.getDeletionTime();
            for(int i=0;i<emails.size();i++){
                Email currEmail = emails.get(i);
                if(deletionTimes.get(i).isBefore(currDateTime)){
                    user.removeFromDeleted(currEmail.getId());
                    emails.remove(currEmail);
                    if(currEmail.removeAlink() <= 0){
                        emailService.deleteEmail(currEmail.getId());
                    }else{
                        emailService.addEmail(currEmail);
                    }
                }
            }
            if(user != null)
                userService.saveUser(user);
        }
        System.out.println(emails.toString());
        return new ResponseEntity<>(emails, HttpStatus.OK);
    }

    @DeleteMapping("/email/delete/{email}/{id}/{folderName}")
    @Transactional
    public ResponseEntity<?> deleteEmail(@PathVariable("id") Long id, @PathVariable("email") String email, @PathVariable("folderName") String folderName) {
        User user = userService.findUser(email);
        Email currEmail = emailService.findEmail(id);
        if (folderName.equals("trash")){
            user.removeFromDeleted(id);
            if(currEmail.removeAlink() <= 0){
                emailService.deleteEmail(currEmail.getId());
            }
            userService.saveUser(user);
        }else{
            user.deleteEmail(id, folderName, userService);
            emailService.addEmail(currEmail);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/email/getAll")
    public ResponseEntity<List<Email>> getAllEmails(){
        return new ResponseEntity<>(emailService.getAll(), HttpStatus.OK);
    }


    @GetMapping("/contact/getAll")
    public ResponseEntity<List<Contact>> getAllContacts(){
        return new ResponseEntity<>(contactService.getAll(), HttpStatus.OK);
    }

    @PostMapping("/contact/add/{email}")
    public ResponseEntity addContact(@RequestBody Contact contact, @PathVariable String email){
        System.out.println(contact.toString());
        User user = userService.findUser(email);
        user.addContact(contactService, contact);
        userService.saveUser(user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/contact/get/{ids}")
    public ResponseEntity<List<Contact>> getContacts(@PathVariable List<Long> ids){
        return new ResponseEntity<>(contactService.findContacts(ids), HttpStatus.OK);
    }

    @DeleteMapping("/contact/delete/{email}/{id}")
    @Transactional
    public ResponseEntity<?> deleteContact(@PathVariable("id") Long id, @PathVariable("email") String email) {
        User user = userService.findUser(email);
        user.deleteContact(contactService, id);
        userService.saveUser(user);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @PutMapping("/contact/update/{field}/{id}")
    public ResponseEntity<Contact> updateContact(@PathVariable String field, @PathVariable Long id, @RequestBody Object change){
        Contact contact = contactService.findContact(id);
        Map<String, Object> res = new ObjectMapper().convertValue(change, HashMap.class);

        if(field.equals("name")){
            contact.setName(res.get("name").toString());
        }else if(field.equals("emails")){
            contact.setEmails(new ArrayList<String>((Collection<? extends String>)(res.get("emails"))));
        }

        contactService.saveContact(contact);

        return new ResponseEntity<>(contact, HttpStatus.OK);  ////////leave it as it is until common grounds is found with front
    }


}
