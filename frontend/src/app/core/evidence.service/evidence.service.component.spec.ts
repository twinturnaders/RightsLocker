import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EvidenceServiceComponent } from './evidence.service.component';

describe('EvidenceServiceComponent', () => {
  let component: EvidenceServiceComponent;
  let fixture: ComponentFixture<EvidenceServiceComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EvidenceServiceComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EvidenceServiceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
