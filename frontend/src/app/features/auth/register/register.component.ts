import { Component, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NgIf } from '@angular/common';
import { AuthService} from '../../../core/auth.service';


function match(field:string, confirm:string){
  return (group: AbstractControl): ValidationErrors | null => {
    const a=group.get(field)?.value, b=group.get(confirm)?.value;
    return a && b && a!==b ? { mismatch:true } : null;
  };
}


@Component({
  standalone: true,
  selector: 'rl-register',
  imports: [ReactiveFormsModule, NgIf, RouterLink],
  templateUrl: 'register.component.html'
})
export class RegisterComponent{
  fb=inject(FormBuilder); auth=inject(AuthService); router=inject(Router);
  loading=false; error='';
  form=this.fb.group({
    email:['',[Validators.required,Validators.email]],
    displayName:['',[Validators.required,Validators.minLength(2)]],
    password:['',[Validators.required,Validators.minLength(8)]],
    confirm:['',[Validators.required]]
  }, { validators: match('password','confirm')});
  get email(){return this.form.get('email')!}


  submit(){
    if(this.form.invalid) return; this.loading=true; this.error='';
    const { email, password, displayName } = this.form.value as any;
    this.auth.register(email,password,displayName).subscribe({
      next:()=>this.router.navigateByUrl('/evidence'),
      error:e=>{ this.error=e?.error?.message||'Registration failed'; this.loading=false; }
    });
  }
}
