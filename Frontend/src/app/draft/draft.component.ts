import { Component, OnInit } from '@angular/core';
import {UserService} from "../service/user/user-service.service";
import {Router} from "@angular/router";
import {AuthGuard} from "../guards/auth.guard";
import {CacheService} from "../service/cache/cache.service";
import {User} from "../models/user/user";
import {EmailService} from "../service/email/email.service";
import {Email} from "../models/email/email";
import {SortService} from "../service/sort/sort.service";

@Component({
  selector: 'app-draft',
  templateUrl: './draft.component.html',
  styleUrls: ['./draft.component.css']
})
export class DraftComponent implements OnInit {
  EMAILS: any;
  page: number = 1;
  count: number = 0;
  tableSize: number = 11;
  draft: number[] | undefined = [];
  reload: boolean | undefined = false;
  key: any;
  sort: any = '';

  constructor(private userService: UserService, private emailService: EmailService, private router: Router, private authGuard: AuthGuard, private cache: CacheService, private sortService: SortService) { }

  ngOnInit(): void {
    if (!this.authGuard.isSignedIn) {
      this.router.navigateByUrl('').then();
    }
    this.getPosts();
  }

  getPosts(){
    if (this.cache.draft === undefined || this.reload) {
      this.userService.user!.subscribe((data: User) => {
        this.draft = data.draft;
        this.userService.getEmails(this.draft!, "draft", this.userService.email!)?.subscribe((response: any) => {
          this.EMAILS = response;
          this.cache.draft = this.EMAILS;
        });
      });
    }
    else {
      this.EMAILS = this.cache.draft;
    }
    this.reload = false;
  }

  onTableDataChange(event: any) {
    this.page = event;
    this.getPosts();
    this.sortService.sortFactory('dateNew', this.EMAILS);
  }

  deleteEmail(email: any) {
    this.EMAILS.splice(this.EMAILS.indexOf(email),1);
    this.userService.deleteEmails(this.userService.email, email.id, "draft").subscribe();
  }

  openDraft(email: Email) {
    this.emailService.to = email.toWho;
    this.emailService.body = email.body;
    this.emailService.subject = email.subject;
    this.emailService.id = email.id;
    this.router.navigateByUrl('home/compose').then();
  }

  search(key: any) {
    const res : any = [];
    for (const email of this.EMAILS) {
      if (email.toWho.toString().toLowerCase().indexOf(key.toLowerCase()) !== -1
        ||  email.subject.toLowerCase().indexOf(key.toLowerCase()) !== -1
        ||  email.body.toLowerCase().indexOf(key.toLowerCase()) !== -1
        ||  email.date.toLowerCase().indexOf(key.toLowerCase()) !== -1) {
        res.push(email);
      }
    }
    this.EMAILS = res;
    if (res.length === 0 || !key) {
      this.getPosts();
    }
  }

  sortEmails() {
    this.sortService.sortFactory(this.sort, this.EMAILS);
  }
}
