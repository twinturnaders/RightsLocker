import { Component } from '@angular/core';

@Component({
  standalone: true,
  selector: 'rl-about',
  template: `
  <h2>About RightsLocker</h2>
  <p>RightsLocker preserves original media, computes cryptographic hashes, logs chain-of-custody events, and creates redacted derivatives for safe sharing.</p>
  <ul>
    <li><b>Originals:</b> WORM-style storage; never mutated.</li>
    <li><b>Hashes:</b> SHA-256 at ingest; on-demand verification.</li>
    <li><b>Redaction:</b> auto + manual review; export redaction maps.</li>
    <li><b>Shares:</b> short-lived, policy-bound links.</li>
  </ul>
  `
})
export class AboutComponent {}
