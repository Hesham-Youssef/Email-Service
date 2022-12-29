import { Component, OnInit } from '@angular/core';
import {UserService} from "../service/user/user-service.service";
import {User} from "../models/user/user";
import {Router} from "@angular/router";
import {AuthGuard} from "../guards/auth.guard";
import {CacheService} from "../service/cache/cache.service";

@Component({
  selector: 'app-sent',
  templateUrl: './sent.component.html',
  styleUrls: ['./sent.component.css']
})
export class SentComponent implements OnInit {
  EMAILS: any;
  page: number = 1;
  count: number = 0;
  tableSize: number = 11;
  sent: number[] | undefined = [];
  reload: boolean | undefined = false;

  constructor(private service: UserService, private router: Router, private authGuard: AuthGuard, private cache: CacheService) { }

  ngOnInit(): void {
    if (!this.authGuard.isSignedIn) {
      this.router.navigateByUrl('').then();
    }
    this.getPosts();
  }

  getPosts(){
    if (this.cache.sent === undefined || this.reload) {
      this.service.user!.subscribe((data: User) => {
        this.sent = data.sent;
        this.service.getEmails(this.sent!, "sent", this.service.email!).subscribe((response: any) => {
          this.EMAILS = response;
          this.cache.sent = this.EMAILS;
          console.log(response);
          
        });
      });
    }
    else {
      this.EMAILS = this.cache.sent;
    }
    this.reload = false;
  }

  onTableDataChange(event: any) {
    this.page = event;
    this.getPosts();
  }

  deleteEmail(email: any) {
    console.log(this.EMAILS.indexOf(email));
    this.EMAILS.splice(this.EMAILS.indexOf(email),1);
    this.service.deleteEmails(this.service.email, email.id, "sent").subscribe();
  }
}
