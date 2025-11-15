# Race Photos Domain Glossary

## Domain Overview
- **Purpose:** Allow event participants to quickly discover, preview, and purchase photographs that feature them by matching an uploaded reference photo (e.g., selfie) against the catalog of official event photos.
- **Matching Approach:** Photographers' uploads are preprocessed through a facial recognition service to produce face embeddings (vectors). Participant uploads are converted into embeddings and compared against stored vectors scoped to the specific event.
- **Commerce Model:** Users preview low-resolution, watermarked photos, then purchase individual images or bundles. Pricing varies per photographer. Race Photos retains a commission on every sale and periodically pays photographers their earnings.

## Primary Actors
- **Participant (End User):** Registered event attendee allowed to browse and buy only the photos from events they took part in.
- **Photographer:** Supplies original event photos, defines per-photo or per-package pricing, and receives payouts minus Race Photos' commission.
- **Race Photos Admin/Finance:** Configures default commissions, overrides photographer-specific commission rates, monitors payouts, and maintains integrations.
- **Event Organizer / Registration Provider:** Third-party service that validates which participants are eligible for each event's gallery.
- **Recognition Service:** External service (e.g., AWS Rekognition) generating vectors from images for matching.
- **Payment Providers:** Secure payment methods (Apple Pay, Google Pay, etc.) handling customer purchases.

## High-Level User Journey
1. Participant authenticates and selects an event they attended (eligibility enforced via organizer integrations).
2. Participant uploads a selfie/reference photo.
3. System generates an embedding for the uploaded face and searches embeddings belonging to the chosen event.
4. Matching photos are displayed as low-res, watermarked previews with photographer attribution and pricing options.
5. Participant selects photos (individually or bundles) and pays via a supported payment provider.
6. Delivery pipeline provides high-resolution, non-watermarked assets post-purchase while recording revenue, commissions, and payout obligations.

## Photo Ingestion & Matching Pipeline
1. **Upload:** Photographers upload original event photos (possibly multiple faces per photo).
2. **Preprocessing:** Each photo is analyzed by the recognition service; every detected face produces a vector. Store vectors with references to the original image, detected bounding box, photographer, and event.
3. **Indexing:** Embeddings are persisted in a vector store partitioned by event to ensure searches remain scoped.
4. **Participant Search:** User-uploaded photo generates a vector which is compared against the event's embeddings to return candidate matches ranked by similarity score.
5. **Preview Generation:** Matching photos are transformed into downsized, watermarked assets before being shown to the participant to discourage unauthorized use.

## Access Control & Event Eligibility
- Only verified participants can access a given event's gallery. Verification uses data from event organizers or registration platforms (e.g., roster APIs, ticket imports).
- Access must be limited to events the participant actually attended; cross-event browsing is disallowed unless permission is granted.
- Photographers can upload and manage only the events they are assigned to, unless granted global access by admins.

## Pricing, Commissions & Payouts
- **Photographer Pricing:** Each photographer defines their rates (per photo, bundle, or tiered). Pricing metadata should be stored per photo or per event package.
- **Race Photos Commission:** A default platform commission applies to every sale, with optional photographer-specific overrides.
- **Purchase Flow:** Shopper selects photos, price is computed (photographer price + commission). Secure checkout occurs via Apple Pay, Google Pay, etc.
- **Settlement:** Once payment succeeds, revenue splits into photographer earnings and Race Photos commission. Photographer earnings accumulate until scheduled payouts.
- **Payout Cycle:** At defined intervals, Race Photos issues payouts to photographers, deducting any platform commissions and fees.

## Key Domain Entities & Glossary
| Term | Description |
| --- | --- |
| **Event** | A managed race/competition with associated participants, photographers, and photos. |
| **Participant Profile** | Represents a person permitted to access specific events; linked to registration data. |
| **Photographer Profile** | Contains identity, payout preferences, and pricing rules per event or photo. |
| **Photo Asset** | Original high-resolution image uploaded by a photographer; includes metadata (event, photographer, timestamps). |
| **Preview Asset** | Downscaled, watermarked derivative shown to participants prior to purchase. |
| **Face Embedding / Vector** | Numerical representation of a detected face used to compute similarity. |
| **Vector Store / Index** | Storage layer holding embeddings per event with references back to photo assets. |
| **Search Request** | Participant-submitted reference photo plus selected event that triggers vector generation and matching. |
| **Match Result** | Ranked set of preview photos returned from the search, including similarity score and pricing info. |
| **Cart / Order** | Collection of selected photos awaiting checkout with computed total price and commission splits. |
| **Commission Rule** | Default or photographer-specific percentage cut retained by Race Photos. |
| **Payout Batch** | Aggregated disbursement of photographer earnings for a period. |
| **Event Eligibility Source** | External system providing proof of participation (registration feeds, API integrations). |

## Integrations & External Dependencies
- **Recognition Service:** Must support batch processing and secure storage of embeddings; ensure compliance with biometric data regulations.
- **Payment Providers:** Apple Pay, Google Pay, and potentially other PCI-compliant processors for web/mobile checkout.
- **Event Registration Services:** APIs or data feeds from event organizers to import participant rosters and validate access tokens.

## Operational Considerations
- **Data Privacy:** Handling biometric data (face vectors) requires strict security, retention policies, and potentially user consent management.
- **Watermark Strategy:** Preview images must be unusable as final assets but still recognizable for selection.
- **Scalability:** Matching should handle large events with thousands of photos without latency spikes.
- **Auditability:** Track which vectors belong to which photos, photographers, and events for troubleshooting and takedown requests.

## Open Questions
1. How long should embeddings and photos be retained after an event concludes, and what are the compliance requirements for biometric data deletion?
2. What refund policies apply if a participant purchases incorrect photos or is unsatisfied with quality?
3. Are there bundle/discount rules beyond per-photo pricing (e.g., "buy all photos from an event" pricing tiers)?
4. Which payout methods (ACH, PayPal, etc.) are supported for photographers, and what is the standard payout frequency?
5. Do event organizers require reporting or revenue-sharing insights, and if so, what data must be exposed?
