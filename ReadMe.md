# Soundbound Extension Lib for sources.

> ## 🟢 LICENCE / ToS ARE NOT BLOCKERS — owner decision (2026-11-07, reaffirmed 2026-11-10)
> SoundBound is a research / hobby project and will be **open sourced**. **Licence is not an issue anywhere.**
> - GPL / AGPL / LGPL / MIT / unlicensed code: **read, port, copy, benchmark, spike and build freely** (keep attribution in NOTICE / CREDITS).
> - Unofficial / reverse-engineered APIs, ToS-grey routes, scraping, token minting, decryption research, P2P, proxies: **all in scope**.
> - **Never** skip, downgrade or rewrite a better option "for licence reasons", and **never** write a bare "avoid".
>   Research → spike/benchmark live → conclude **adopt / adopt later / not worth it** with evidence (quality, coverage, reliability, effort).
> - Still: no secrets in public repos or client builds; respect rate limits (MusicBrainz ≈1 req/s); prod stays read-only unless the owner says otherwise.
> Details: "Research Scope" section in the app repo's AGENTS.md.


---
#### **Sources Repo for Personal Experimentation and Educational Learning Experience.** [disclaimer](https://github.com/Shabinder/soundbound-extensions-lib/edit/master/ReadMe.md#-disclaimer).
---
Click on this button   :   <a href="https://shabinder.in/private-repo"><img src="https://img.shields.io/badge/install-black.svg?style=for-the-badge&logo=addthis&logoColor=white"/></a>
<br/>
<br/>
**OR**

Manually:

 - `copy and add to Soundbound App, in Settings -> Add Repo.`
 
https://gitlab.com/shabinder/soundbound/-/raw/main/Experimental-Sources/sources.json

---

## Optional capabilities

### Skip segments (`Provider.getSkipSegments`)

An extension may return spans of a track that the player can skip automatically, as
`List<SkipSegment>` (`startMs`, `endMs`, `category`, `actionType`, `id`), and sets
`isSkipSegmentsAvailable = true`. Both have defaults (empty list / false), so existing extensions
need no change. Extension bundles
built before this function existed do not have it, and the host's call fails with
`ZiplineApiMismatchException`; hosts must treat any failure as "no segments". The contract is
provider-neutral: `category` is a free-form label, and the app acts only on `actionType == "skip"`.

---

## 📃 Disclaimer
All downloading and use of the software and its accompaniments are deemed to have carefully read and fully agreed to the following terms:
* The software is only for personal learning and communication, and it is strictly prohibited for any foul and illegal purposes.
* If any third party commercial behavior and illegal uses are found, the software author has the right to revoke the right to use.
* The risk of using this software will be entirely borne by the user, and the software author will not bear any responsibility.
* Except for the terms of service indicated in the software, the author of this software is not responsible for any accidents, negligence, contract damage, defamation, copyright or other intellectual property infringement and any losses caused by improper use of this software, and also no legal responsibility is assumed.
* The author of the software will not be responsible for the interruption of services or other defects caused by force majeure or uncontrollable reasons such as hacker attacks, communication line interruptions, etc., which cause users to fail to use normally but will try their best to reduce losses or impacts caused to users.
* For issues not covered in this statement, please refer to relevant national laws and regulations. When this statement conflicts with relevant national laws and regulations, the national laws and regulations shall prevail.
* The copyright of this software statement and its right to modify, update and final interpretation belong to the software author.
