# Login Asset Migration Manifest

Date: 2026-07-28

| Flutter source (read-only) | Native destination | SHA-256 source | SHA-256 destination | Consumer | Result |
| --- | --- | --- | --- | --- | --- |
| `E:\vista\lib\utils\images\logo\black-logo.png` | `core/designsystem/src/main/res/drawable-nodpi/vista_auth_logo_light.png` | `13A42B455E836D64661E4E07C6DA8D3A25EE5213745F00CCADD3D827DAC2795C` | same | `VistaAuthLogo` / light Login | byte-identical |
| `E:\vista\lib\utils\images\logo\logo-white.png` | `core/designsystem/src/main/res/drawable-nodpi/vista_auth_logo_dark.png` | `6DF4233FD7BD60B1DCE2B76A6904B9582F8572263391085CBC54B1A476EAB510` | same | `VistaAuthLogo` / dark Login | byte-identical |

## Existing byte-identical font reconciliation

The Native files were already present; the audit confirmed byte identity, so they were retained rather than copied again.

| Flutter source | Native destination | SHA-256 |
| --- | --- | --- |
| `VazirmatnBlack.ttf` | `res/font/vazirmatn_black.ttf` | `DF4E257BE9DE1574D11BA2970C69228A7DB24174913FF7F776BBF00A9AA5DC1B` |
| `VazirmatnExtraBold.ttf` | `res/font/vazirmatn_extrabold.ttf` | `5181C2E436CF1D5CF1C5631821B43D81881DCA040A12C9A442EF5DC0007001CF` |
| `VazirmatnBold.ttf` | `res/font/vazirmatn_bold.ttf` | `F635FDBEA28F265DE395BA83B4B1570DCF2F58D13C65469E61903B1C2D2AE723` |
| `VazirmatnSemiBold.ttf` | `res/font/vazirmatn_semibold.ttf` | `3F239D8364C14EE32C46B6839FA197C28958358B82A9673D2C6ACC8506D78BC5` |
| `VazirmatnMedium.ttf` | `res/font/vazirmatn_medium.ttf` | `B986623E4DDEF10755E04BE39F8EA7BCB1DC08BFE8DD0AA6AF395736F256AD4A` |
| `VazirmatnRegular.ttf` | `res/font/vazirmatn_regular.ttf` | `B69FD4C680B8F3F225FEABCC655A2C585D97627B8F5F5C0F9985E894069F3A56` |
| `VazirmatnLight.ttf` | `res/font/vazirmatn_light.ttf` | `9AB4F094B861B7D4E318F01953B9771DDEB189D7C79BBB3989D5A95A4B07FF60` |

## Material icons

Flutter uses Material icons for Login controls. Native uses vector equivalents for person, lock, visibility, visibility-off, and `ic_auth_arrow_back`. These are not substitutions for custom Vista artwork.

No automatic tint is applied to the color logo assets. No asset was downloaded, rasterized, or recompressed.
