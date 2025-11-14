import { TestBed } from '@angular/core/testing';

import { EvidenceApi} from './evidence.service';

describe('EvidenceService', () => {
  let service: EvidenceApi;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(EvidenceApi);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
