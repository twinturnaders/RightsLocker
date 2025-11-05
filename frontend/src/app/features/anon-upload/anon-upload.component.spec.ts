import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AnonUploadComponent } from './anon-upload.component';

describe('AnonUploadComponent', () => {
  let component: AnonUploadComponent;
  let fixture: ComponentFixture<AnonUploadComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnonUploadComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AnonUploadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
