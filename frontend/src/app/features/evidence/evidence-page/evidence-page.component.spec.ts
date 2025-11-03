import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EvidencePageComponent } from './evidence-page.component';

describe('EvidencePageComponent', () => {
  let component: EvidencePageComponent;
  let fixture: ComponentFixture<EvidencePageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EvidencePageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EvidencePageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
