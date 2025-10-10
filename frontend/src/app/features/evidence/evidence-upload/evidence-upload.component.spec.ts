import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EvidenceUploadComponent } from './evidence-upload.component';

describe('EvidenceUploadComponent', () => {
  let component: EvidenceUploadComponent;
  let fixture: ComponentFixture<EvidenceUploadComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EvidenceUploadComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EvidenceUploadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
