import {Component, inject} from '@angular/core';
import {HttpClient, HttpEventType} from '@angular/common/http';
import {FormsModule} from '@angular/forms';
import {NgIf} from '@angular/common';

@Component({
  selector: 'app-convert',
  imports: [
    FormsModule,
    NgIf
  ],
  templateUrl: './convert.component.html',
  styleUrl: './convert.component.css',
  standalone: true
})

export class ConvertComponent{
  http=inject(HttpClient);
  file?:File; blur=false; uploading=false; progress=0; ok=false; msg=''; readyUrl='';
  key=''; title='';


  pick(e:Event){ const f=(e.target as HTMLInputElement).files?.[0]; if(!f) return; this.file=f; this.title=f.name; this.msg=''; this.progress=0; }


  reset(){ this.file=undefined; this.progress=0; this.ok=false; this.msg=''; this.readyUrl=''; this.key=''; }


  async start(){
    if(!this.file) return; this.uploading=true; this.msg=''; this.readyUrl='';
// 1) presign
    const presign:any = await this.http.post('/api/evidence/presign-upload',{
      filename:this.file.name, contentType:this.file.type||'application/octet-stream'
    }).toPromise();
    this.key = presign.key;


// 2) PUT to S3
    await new Promise<void>((resolve,reject)=>{
      this.http.put(presign.url, this.file, { reportProgress:true, observe:'events' as any })
        .subscribe({
          next: ev=>{
            if((ev as any).type===HttpEventType.UploadProgress){
              const p=ev as any; this.progress=Math.round(100*(p.loaded/p.total));
            }
          },
          error: err=>reject(err),
          complete: ()=>resolve()
        });
    });


// 3) finalize (server computes hash, enqueues jobs)
    const ev:any = await this.http.post('/api/evidence/finalize',{
      key:this.key, title:this.title, description:'Public convert', capturedAtIso:null
    }).toPromise();


    this.msg='Uploaded! Processing…';


// 4) poll until redacted is present (simple polling)
    let tries=0; let found=false;
    while(tries++<30 && !found){
      const cur:any = await this.http.get(`/api/evidence/${ev.id}`).toPromise();
      if(cur.redactedKey){ found=true; break; }
      await new Promise(r=>setTimeout(r,2000));
    }


// 5) ready: package URL
    this.readyUrl = `/api/evidence/${ev.id}/package?type=redacted&includeThumb=true`;
    this.ok=true; this.msg='Ready!'; this.uploading=false;
  }
}
