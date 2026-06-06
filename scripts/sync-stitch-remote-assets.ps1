param(
  [string]$ProjectId = "13275961100622290348"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$stitchRoot = Join-Path $root ".stitch"
$assetRoot = Join-Path $stitchRoot "remote-assets"
$screenDir = Join-Path $assetRoot "screens"
$sourceDir = Join-Path $assetRoot "sources"
$webAssetRoot = Join-Path $root "web\assets\stitch-remote"
$webScreenDir = Join-Path $webAssetRoot "screens"
$webSourceDir = Join-Path $webAssetRoot "sources"

New-Item -ItemType Directory -Force -Path $screenDir, $sourceDir, $webScreenDir, $webSourceDir | Out-Null

$screens = @(
  @{
    id = "9316e37facb5477e8a41f17c92caf433"
    title = "M&D Social Feed - Polished Set v3"
    device = "DESKTOP"
    width = 2560
    height = 3616
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLuo21mFZL40EXgzAKtOc_bQToXLLLYCxWsaXl2GjRTdRnzxRtJEPLQeK6RCy8uSw1XEArf6-hCKsMqgVHFmzaTtuTer3oApPLWZquYKi-40qNNfRoNddXTJX9OqO3c6ukRMn_fwIb_GqDZNmagbEO9bXd7J0IhE9-2t9vNSnMVozlC1Cw7zCiBuJBSjFpaccfnbQlJIwga9e0bZyoqkR13lxJVDiauNPKV8FipYEnRsOTLdE6gQiNai5z4"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzcyODUwNTFkMWYwMmQzYzIyZDliMTg0ZWUxEgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "web-desktop-flow"
  }
  @{
    id = "e57b8cd42f6f45c08949322cbcc3b89f"
    title = "Mobile Market (Polished)"
    device = "MOBILE"
    width = 780
    height = 2188
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLtyXz5f9oVbNZtalEHB3ROQ8BMFSR6LCXqF-T3LRW2Skjc-SCe31FRzddgKCSnAl5C2tCNYv6EMJfabxL65lkJLlaN7miI7HAHkW_2wNBzrn1EiQcDFYqZ1ITekH1-PA8QtBZ5QBlDkeQbqLbXuClyY0PGqwC_KGh5sAcYUQK9EvDhc-w4sAu_7QaZoB3kqJpIuxnZyHBwwFXk7txToV_l8DU_RORFlna__h_Z250fNC_eiFC1wjyp0tDSX"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzcyODQ2ZWM4MzMwMzgzOTgwMzMzMmY4ODljEgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "mobile-flow"
  }
  @{
    id = "fd8cd509a0c24d61b789ae22f4146c80"
    title = "M&D Review & Safety - Final Polished Set (No Nav)"
    device = "DESKTOP"
    width = 2560
    height = 5320
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLu6pdddHpa5o-0UvigqAKtwZiD9uESjBG61KKzG-HBv6ens-H60D9rdWSBtDeVowE-XwYm5cEdjMtRl5mzMneASFNKr8gzefhpQC5jyRZPp33SjpzGta9ioqE9LON3j8FjC_7anI7idasLzj_ttRoV344DZfpG_eu9x3eRXfAnVrU3d8a5QkGRdUiTKvVhT-qeE7bkZBYpvsZosvx0Hhk6DZC4SEmbFvzbJBIBEzG7Uw8GLL9TXnUQuhPg2"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzcyODRjMjE0NzYwODlhZjdiNDM4MTYzY2YwEgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "web-desktop-flow"
  }
  @{
    id = "bc706d4088e34bdfb6c30bf8045e4cb4"
    title = "M&D Design System - Phase 1 Foundation (Precise Tokens)"
    device = "DESKTOP"
    width = 2560
    height = 4574
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLui6izdcq3yrWiDedPpQwpvdv4SSRb-G1g1IP0TzBVJu0P9jsnZfVtWK0ShStqb2XR8WlMysJTL6aEBD97EY18bxm28em1sIytNNt3yDRfuCtLHAEbGvrhlgVUXQ7EVPs78Bop1fTBuxtnpNy_3duDrP1F8WAfIFDANHrubPBy-FwPKwOcNP17TFcj_2rId9UPSOXruk3PTIjvHu38mG3dvbhSdDYcha4zw6CKXIC0JRTDCiVg6a3xG7Zi5"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzcyODQ1N2Q1ZDYwMzMyZmE2ZjkxMWZjOTllEgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "design-system"
  }
  @{
    id = "d940b93dce0547dfb353990cf6c90719"
    title = "Mint Theme Color Board (Updated)"
    device = "DESKTOP"
    width = 2560
    height = 2336
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLu4asJPXksQvZf-gZBEAgGiXMUWcEieWZy71SyqHcfzVnOCbVBvbhxA68U7jv6lCtcbWSzxeIV89geXyUIkXiCVWKHcy5_1fINQuu7vIqGSs-Ttgpuy6ng5AoB8gv66kCz0ijuC7s-SvDfaZm4JBUyv7Bc8tIwxuQQ6aqgunGsRHO2hiNl3RliP6lzob-iLNeNJ-MXaC-1PxYnqVLbhdNfhFpiMtOJA5aPCBc-yCRVEEh9KqoxQ6oNPHkMg"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzcyODQyMTBjODQwNTRjYzMyYTVkMDM4YjI1EgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "theme-board"
  }
  @{
    id = "c9e36d888ec147a692112ba1da15233a"
    title = "M&D Component Library - Honey Basic Components (Full Spec)"
    device = "DESKTOP"
    width = 2560
    height = 3940
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLtaBKpQACBmFm-rS24tzwhHgZXLFsT2vjM_SyTyV0EcRE7k9W_ybX_b5R_uOm3FljeN2AyMoU7gitysiFmwBSmjMzLcTo0ZtjUIl5zZJ95aw0yT-U9cEF0YsBiU2S-qb_CaBmDC6kEb9iTdNI4XLKnkkQbL22dirvrBHdX9i0KHFIrv7SW0IE2kEchYLMdcqLQK0JK84JvsUQw_eAtFENeUoDtyVzINuQXPudloc7feZASA4VvUoYZrB22x"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzcyODNlNTc0ZDUwMjA3Yjg5Y2Y4MDI4NjlkEgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "component-library"
  }
  @{
    id = "5e8d58afba4142068ea51010b1e17e3a"
    title = "Ginger cat reference photo"
    device = "IMAGE"
    width = 1248
    height = 832
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLs4-hnL88eETQ5apBrhPNTpS2qhWxVqD294cJf8Sna3zrv2VXOu0bVClGi0aIV8e7FUquU6RxfZ47s4xcTwLyvm0ZwHiV2BfIQN3MMDbe-Q3oygYyyE97RAch-XY0GL9qTUbuAgZXfcQLoq99wtZphhwn0AJx49etUxwfm6j3WxiVKP1ab8p1ydnsgEr4J6odB5oaKz-DUMKLPHuloFGnKCqameFfplcDJPlSNB_3JJ8wlkXY1F3A9a3gQj"
    source = ""
    mime = ""
    category = "image-asset"
  }
  @{
    id = "dad492ad9bc547e4aafb94b9a9baf50e"
    title = "M&D Design System - Geometry Layout and Business Icon Rules (Honey)"
    device = "DESKTOP"
    width = 2560
    height = 3290
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLu6C9SoZZfDslNIi88fud80r1uoFTFjqkaJS1KwJ_SUUkCtQPRJGK_PZL_vTnVa4CzBr2UFLsxZehODLUt4Ga5uUnBy9g9-_mDFaXtp8bips6xxQ2p7JV0Vk5o9jNlkgXhvjh_hO93DPnVSlYMjB9T1ZoE120ugx11cCcdHKzmrZ9iMC26uinfAkPJv7dToW3XYgdY_J9yBTMdGKHWJLZarEjS6mhom6j7CVla1NezfFCeoGiw58LHoB7k9"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzcyODRlNWM2NjMwMzM4NThlNzI4MDliYTQ1EgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "design-system"
  }
  @{
    id = "b8257bab8a6c4b28adaf0a9c209fec36"
    title = "WeChat Icon"
    device = "ICON"
    width = 512
    height = 512
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLsAur-LdeTwTpV087xzd4pofpW4h9PiODWs17kiFfDFYralomNQsuPXumM5CWcioWyqkYv6pCoBPn77pcJ3iFLsEHvOPdNyaV8fRYVbQr55U9yWMknTnnlNzeQ14_NGCQhpr5sIC9PfqbhnXJBt6wULkOq6GMDkDRGaP8G60A9Iat4FeFzY17bOdSg5WAqHr9k2vK5J0u8FW4l64B5E72dhSqnKAxmZrvaORoQeX71XHcaZdEeahGpAeMWA"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzUyNjU5NThiMWJkOTQ0ZGFiNzIwNmE5ODVmZmI0YzJhEgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "image/svg+xml"
    category = "icon"
  }
  @{
    id = "6d735a2d4f714b29ad07607685e2de49"
    title = "Mobile Profile (Polished)"
    device = "MOBILE"
    width = 780
    height = 1902
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLtpXyEfjXqTU8XSKOL0h8Bsh3lEZTGju6HDiamFhp7dm7GJftsJD_xpsnM2to8Doj_NOMa4aNZzmVlklkpe3S6RrhWkajnxCpUF29QvD9TA3790KRVq6pnef1VGLpgTihFSw-ZDsln_aPiD1I0ELnh4vB0xJ9pgdDTPrTAogY52mamt2LDYTAKtgZWPqLXWk903j_WiRfcfR4nA8wVBlGl26uEpgywoMu0bTkSX58DsHgfgU8AezkHrKYHG"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzY3YmY0NDRmYWEwNjM5NzBmNzdmMDdjYmRkEgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "mobile-flow"
  }
  @{
    id = "d948de8c81044c14936b7294260f7ac6"
    title = "Apple Icon"
    device = "ICON"
    width = 512
    height = 512
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLuPnCBgEItGwvqw4QfcSeb5_znURKWF8Sw_U7TG6LGw19_J-hAze9-fATblXzeuWFRMGGNbd3Aw20pdifgBaOgZAa5OFpAMhpOqm4CdFMGH1juKOlZadpmvNZLi-zc8HDEzJCcjwxSP_FynMrDsiyQ0GZwOtgZATuhMH4PvVKv2MLXokrn27XSNN_XrfZG4ToJ42qyvD5ekJ0NzjLwCND4G1GsiRZeXpNjSgVEcMF-5gzsW7v9GeieNVInm"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzJiNGE2YjZkYWQ5MTQyOTViOTIwMDg4ODBhZDg3NDk1EgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "image/svg+xml"
    category = "icon"
  }
  @{
    id = "09f6cf9adfbd4f1a9718735e988ce374"
    title = "Google Icon"
    device = "ICON"
    width = 512
    height = 512
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLuMryU5Zng8RBmu2R1rLTzjtMxVy3DDeWZMZOvr-ovAFeUwIIWU5ttgff5hWAXQ-9AGs0ipPppr98QFbXoDCawrrWJcP3l8U-sU9qNC2qov9_IPsC7LhRaai76v2lZ5H_3Oa1LG3zgncKzVhK9uky96K3559f0HBKcRGRPitLBKt3Tyw01SNrJDEg1UdNnErS17Yt7P1UOTZAKbUCxQf1SsVUojdIr0F4KwokOwlG9bEpS3jFYgVOGp05Xp"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzBiZmY1MzIyMTYyMDQwOWY5Yjc1OGIzZGZlMGU5YWY4EgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "image/svg+xml"
    category = "icon"
  }
  @{
    id = "1187049162856747640"
    title = "MD_GLOBAL_STITCH_DESIGN_SYSTEM_V2_NEUTRAL_FIXED.md"
    device = "DOCUMENT"
    width = 780
    height = 1768
    screenshot = ""
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBKOARIhYXBwX2NvbXBhbmlvbl91c2VyX3VwbG9hZGVkX2ZpbGVzGmkKM3VzZXJfdXBsb2FkZWRfaHRtbF8wMDA2NTM3MGJkNWM5N2E1MDJkM2MyNGJiNTE2MGU5YxILEgcQgtO97aQBGAGSASQKCnByb2plY3RfaWQSFkIUMTMyNzU5NjExMDA2MjIyOTAzNDg&filename=&opi=89354086"
    mime = "text/markdown"
    category = "global-requirements"
  }
  @{
    id = "11388975682457720262"
    title = "MD_GLOBAL_STITCH_DESIGN_SYSTEM_OFFICIAL_FORMAT.md"
    device = "DOCUMENT"
    width = 780
    height = 1768
    screenshot = ""
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBKOARIhYXBwX2NvbXBhbmlvbl91c2VyX3VwbG9hZGVkX2ZpbGVzGmkKM3VzZXJfdXBsb2FkZWRfaHRtbF8wMDA2NTM2MzdhMmNkNDAxMDkxMDRmYTNjYTNiYzhhYhILEgcQgtO97aQBGAGSASQKCnByb2plY3RfaWQSFkIUMTMyNzU5NjExMDA2MjIyOTAzNDg&filename=&opi=89354086"
    mime = "text/markdown"
    category = "global-requirements"
  }
  @{
    id = "16054956948476331741"
    title = "icon.png"
    device = "IMAGE"
    width = 1024
    height = 1024
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLuo3JhJraKUd7nELphov7BvSAFxLWohjBCmMYAbFgbfEny830CkJ90qCHahvWgbmL2AGWRfBXvqdv9QAV62EtQcJ_5JEI5hW-T94lLaWizeNwgtpLRQPsouJtRWsGRlRbd0ybvkuprJ7Ejuw66JyGhCm4DoSTsp7aFE0hh_2GfSvusVH_Zusr5eMkzYf17K-y-Gl-RBqHbUXydnM2l2gazGeZJrptj8E21FSy3RO4klhF8AqhFAY2wPnRf-o4CUM9zySnFyEksLvFY"
    source = ""
    mime = ""
    category = "image-asset"
  }
  @{
    id = "ec4b9497c04f4b91b2d6236703efdaff"
    title = "M&D Honey Settings Flow - Final Polish Patch"
    device = "DESKTOP"
    width = 2560
    height = 7120
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLs6o59Xwaf_V2WQte47W4IDgMU_NodVjJehFDEdi1x0ShDd08dP07RHOM_pJL8ykZm84BS0bf9nnAoP9MwHkXXK0gNsJ4nzUdvk3N6LNvg5waY6HEUZz62o0mP4ZKEWF8Wg3WfwFusoFdAtTn0jXBXejm_97X9GVH936CHy5dXS6FPwoykoq76Uk2-HwksASpa3IcXB42Vx3_sW7yDf1kFCWrdqqySMRTSDNOtzNyRqXEog28_4lnH9Uqg"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sX2FlMjE3MmVkZDc4ZDRhYWI5M2E3NTI3YzAyMzM5MDY0EgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "web-desktop-flow"
  }
  @{
    id = "bbfc29fa595a4773afc2a91b276b2a05"
    title = "M&D Honey Orders - Final Polished Set"
    device = "DESKTOP"
    width = 2560
    height = 5544
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLtKzqzxUq1YZYF7rLKmfkTFJLTLdPXCGEfwzsjezShSBHc-tJsyeiNU9GUTVT_kplnLeo1s-ZZsICUO3cjPiVcqsYg5z6f4f9vvpcQpatg4OcOpHPOzX4OedXvUpdPGWM8v73Mmec8XNxTLwRH8o8mXbQDgspUUd7jAtKz5O6zK0k_pVloOOuis1rz9PMaa9WhQ4Nj9GUhsmLRBq9vB25m_htdL7V5MM6Q3C-T1QFuMtzJTlOkLV0ZYcvV8"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzcyODQyZWUxYTgwMzgzOGU2NDU4MzYxNGMxEgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "web-desktop-flow"
  }
  @{
    id = "b8877dd4369e479fbbddf4dcc3a5e5eb"
    title = "Mobile Market (Polished v2)"
    device = "DESKTOP"
    width = 2560
    height = 3568
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLsGeZWld7glzuOTtMk_dhTmz8bgzMJDHHDCvMKaCg694ZIXgY_ae_AnepApcuKdaRNTMx5u6aHUxldTdZtwI0tKzjGH8rdnkz2nh5U4w_1qxIT5nezg5jXf9SDEOInmDNRUB6KSgReD-PUN_wPWjhfiSowFHhLvzhBtpQmbmMhgA9xYWe653D_H8J0lLukMIENs8Rw5LpVE949l6-CJ20OpTaxUIQIeLqJNo5ZLiu5YqjXG77vGCiQ2MnRP"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzY3YmZmZDdiNzkwMWE2MmRjMDhiMDVmZTI3EgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "mobile-flow"
  }
  @{
    id = "ab383df88f3f4309a7e55e0bece4a54c"
    title = "QQ Icon"
    device = "ICON"
    width = 512
    height = 512
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLvVGn7rPTBitCD7T7SXJTLi0OWn0y2ShpKYwRt1SdhwY5YbzgR8V-5yfcZW8Vwr7ORukyWivbRV4dxrDBZxjORnKnN-UmyMXBi8Gvqev3Et-QPuLDggdflkQ_Q3Bh4yME8QKTjxKS-q2jo2ipDGRJytHM6RNtDDSWzOgEle09jzkYrCmdnBOzTA4JzglW7YEYtWQzlU0SeW8jMNDPU0XndcFFqY8dUBlLsYnfynCHah2OIvPacSkYlKL1rw"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzM1MDBmZmU4MDIyMTRiNGI5YTJiMzkyODhiYjdiYzJmEgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "image/svg+xml"
    category = "icon"
  }
  @{
    id = "b30ddbe5a0c241fc910ff51716f415c4"
    title = "Honey Theme Color Board (Refined)"
    device = "DESKTOP"
    width = 2560
    height = 2342
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLu4jY-5K1ysBAcT9rKpAp9Yjln4nryO_2iYWrua54WOvV0A3pm_o7aSqolHSZUPGFtO5AJbOP0AJZCB03jPh7pUMrrb5sfSIf1E805VHMU0tec7TPU5Ucvv3p_KPWQ4VlpLYFQCp9xBTJtHuEMTh3YRQ8skI6R3PuYU1u6lW91SAEOep0-TQIH8cxjLEMAQWnaHrp4GZOKhYpsYEKwyO324AFmDcE2bkfro3x-Gh1T6TSit02WUfFylQG2f"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzcyODRjZGQyOTcwMjBjYzA3MDMzMTU1NGY5EgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "theme-board"
  }
  @{
    id = "a984395ba2834be688e397c12967facf"
    title = "Night Theme Color Board (Final)"
    device = "DESKTOP"
    width = 2560
    height = 2840
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLsAeZ0Oh7f844gdFfXlP2RaUmkkbOWAXSo4LvSUVq0JFurrESBwo7FPPlqHSkdPeqNSWGrgEVnpkgvgWLI5Ov9sgp9nbqLqk5qvpLxYev5qCMCzCgfmb3PXeLGvzaqHG-F5vlQdixsUWfbbT88JjJ1v8LblwBUMy_95Q7NQ1TzMqBJk6BsPeG0HoPTlQEtki0tLDMaf4tiAMbEkYEcCIBLz8qaDl1rXjarfeVy-aA_jAga-gRbLlnHXuj8B"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzcyODQ4ZDU5MmEwODI5YjVkMWRmMmVlZDdiEgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "theme-board"
  }
  @{
    id = "11310ac2a2284e809a68589261f12e31"
    title = "Neutral Theme Color Board (Final Refinement)"
    device = "DESKTOP"
    width = 2560
    height = 2136
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLvu4ed3b3AGudYZy0-c4T_TzgbUC7D4lik1fPArCOcCv7cMTlach9klQLXmyJ9NVYPnMotNIxy6w3sWFkrevB4b5H1qpWJhajdITgUpyOP5KjL4bnt-62S_BOgsFidxqrNdD_GMsyNGYrnWh7cS8qORXYtj0GwK6TFNNnHxR3XFcxRepXRz6CpA1x0EfpfLKHVSlxWs2_AVZhPTeDNzGjTdYoVYz5cFK8DToyMwQqalgtHs7YUNRu_fIbY"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sX2Q2YTQ0MTNlMGQ3ZDQ3ZDE5ZDcxODM5MjRhNGU1OTdhEgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "theme-board"
  }
  @{
    id = "eff431efe77f47858d47809096cfad7a"
    title = "M&D Profile & Pet Profile - Polished Mobile Set v4"
    device = "DESKTOP"
    width = 2560
    height = 3568
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLsOfO7I3OfMDqQORpCJuyXc2Z7XAsJC56QXr1MG9t7vBnVmorxYup0PZEf2-hgW_Cg69OhvEB_CiSS9zJDAjchzgzRIBCoSAaNZqkLLRnGvRRUw0VQnVHXW14ChJ66a_nIzG5caS1m_U6OP1a3mnp11PTBorTJVvRQ0NLTDrRCMWYgyr_e8soIv5Wi1WlchGNdtc_-lk2vDscxdws3E8Ir1GR3G3rn4beHB87uFltR4Vdy6NihM0RbZLVqZ"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzY3YmZmZDdiNzkwMWE2MmRjMDhiMDVmZTI3EgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "mobile-flow"
  }
  @{
    id = "44b8b0dc48024ccc982898ef4c900b81"
    title = "M&D Component Library - Honey Messages & Orders Components (Full Spec)"
    device = "DESKTOP"
    width = 2560
    height = 7720
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLvbGAGwTBCM_oHffqumam2FuY-AgR0nrunGXX6MnXOaHv68T6fVgOVPHnInvNvSyZAiCYo98AXFY0OvUVL-XCPy3VkC_GU5YuySsXBdeNdGEnNyYe2ZgHa-xoG389S2-sJmWYLZIgSa4qMdqDvkiiMIyxxOsAZiFhWRqgmXdvN4t2owC7a8TfgfJ0sIVdnG3WTTPCadZXQ_IIaqWoqBXM7FCbLHPK_BrzFbK1_rZVunZKZRAdobflS_IBzH"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzcyODRhYTczNmQwNDMxMWFkMTQ5MjNmMTljEgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "component-library"
  }
  @{
    id = "2cbf0afac9824702b850d2f0d9d06942"
    title = "Mobile Home (Polished)"
    device = "MOBILE"
    width = 780
    height = 2106
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLstDpsDlHyR1ZAW5yOR-54m6KFN9Rmw-64itdKcEavC6_hBgY6RFXeXbn2CB3LZ6kIdF6gdDju-sIRhLrWxRp612BrYpz6J90kzkpkJQWfwRV3aDK5fKylqcYIHgthKktYNiLo3f7dnL3AocsVg7aNsIxfcu-fWp6cW7J2xop1UVqUNCRGr5puWg24BVK5ZIMYgQnBm0qaBADVhWmcodgV_nZieyPb1OgTZekETXmaFDgK0cBeeTsO4RxQ"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzcyODUyMzE4YTEwNmYxYzM0MGE5M2JkYTQ4EgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "mobile-flow"
  }
  @{
    id = "2b6e0ccbec9a41a8b1c11148dd5287d8"
    title = "Board 2: Social Feed Components (Refined)"
    device = "DESKTOP"
    width = 2560
    height = 3774
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLspODXmD117_iWVgb8mljVkoLxn9OLGIt98voTyB7UUnLtTC8DZE4ch0VU1tyBhW6njqquATjkDI8LFS3d28YdUlh0XJUYSyTTOcfa26yG-zWbkwmxyijuNqGtJXa88gc5QlAAAUPnxueXmDqsGg7K-AMl_X2IJHRYh9I69x69KEq499HJNnE-_1-EgJqqOqYlVe_tS28KSJw0YYP3Wa-zDFZkqQ9ww6cSoO5vyJAuoX8S7VQAHsVU7UbDp"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzcyODM5MTlmN2QwNmMyNmZkZDk0MDRiYjkxEgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "component-library"
  }
  @{
    id = "b58af2de47354cf5ab92f5b64b160350"
    title = "M&D Component Library - Mint Market Components (Full Spec)"
    device = "DESKTOP"
    width = 2560
    height = 2140
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLtlevyftksqvf1TmUxP_moRnjFn6bsQSkyFTbJJmBX1oF4aWg0C8j_KHGpDPepCW--aVSnKiyzjMX88zbN75xrq0bzbQP85H0FAN27g4OyEmFxmhXCo3OTzl5GOFANWCqb1MPiKud2rwCQqxO05AfHwj56itUldNKUe1qlsB9tSe70HSpgJIIZDbp7AIkgyt9Vyxif999NY3TuiS7mo8nDL9RYdv3irjEGPWp2WRrbaKwNGMt6Fk2nMrC8"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzcyODRlZjEyOGMwMjA3OTFlZGMxMDgzYTk0EgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "component-library"
  }
  @{
    id = "207a704fbe6e4962b73725ee6d2b88da"
    title = "M&D Night Theme - Final Polished Set (6 Screens)"
    device = "DESKTOP"
    width = 2560
    height = 5448
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLubqtYaxjlX6IminEYJFYwZSSKzzQ5w2wf135qp29JVCV96hgtucnclYIbN17yw75VoG_cdMsAD-_4GrqNZl5xATML_IGeUBfQXORh6O3-QzkK2BqbLcrZ4J4l9eRII_kngXY5uZxMRyC4xoorQRBU_Z8xg_ARdBE7obWi2qyl-8Gh-cp_VxIxJuf9m8P3a2Czr-9rnMbVhYbKgs4VSFi6NOanRMWjHGcHsnIAI2-NBekQTILK-jKf8SFI"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzczYTZmZjJkZjMwMzM4NThlNzI4MDliYTQ1EgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "theme-flow"
  }
  @{
    id = "72bf00c20d734bc7b1312be642a918fc"
    title = "M&D Auth & Onboarding - Polished Honey Set v3"
    device = "DESKTOP"
    width = 2560
    height = 5384
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLtqDKAXVX5CzmTxxQ9ugpdZESQYq3-COeYABmV3wFVPUgraGuTWyG3HGRCBW_WCG4UkSrd3QZHcuFR9auwwLKhoBpgEyYLnozC2OJXku5BMuuYqV1WVzEkd0GGTqODKxgSvLpoxF6djq9VHjSn_Q1B4pMxQBvCoCwnXtpLAn93i_7rE1Fhem6xuch77HBdNgURsrOTEo56Pig43nk8zJqttAg5PfiYPD50003G0AeKMjXEaEKAcvjV770vP"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzcyODNjZjEzYmIwMzc0OWEwZWMwMjBmMjM0EgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "web-desktop-flow"
  }
  @{
    id = "fc4bc839c77a423abb2b1d3c4664d714"
    title = "M&D Interaction Patterns - Advanced Interaction Components (Full Spec)"
    device = "DESKTOP"
    width = 2560
    height = 5746
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLuc9dql_CyKPs5hFFCaJ3TbdUdtCdXOu6x0iuyJEi27eLxj-B4M33JSS1MdPyW6NTkM11o6S7QGtLrIbkezdfD5k9eUFvgq4izdgKIWHl5AkU2AddEXoz9baIZWVrWDMiTCKFRpZbO1urQgjYZEOlXw3iO6dWyWw35anGctAPRt1VC6SQN5tDC45H53z4aSaYemjrJv7v3w__tXiqj3eA_W21fpuv6YrUNd7pDqDfjfJffsvuqnA60yTKVr"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzcyODQ3YzljYzMwNmMyNmZkZDk0MDRiYjkxEgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "component-library"
  }
  @{
    id = "7745202be4e3400fa8556050a89dcca4"
    title = "M&D Component Library - Honey Social Feed Components (Full Spec)"
    device = "DESKTOP"
    width = 2560
    height = 3872
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLvHXOPd8D7BZfjH3qOUd3Lf6xNf5u5av1Wyae9cCKGqSOfJL9fbYvb5H1Ma6mkXLbhsmjxqFXIZrhpsnrqOni6rvnYiYdVzDnmVRKKPtklg1RfuDRnWSrry7pxU_JtCJcjADLR3wPGHr8eE3rrn53tg3VVshPyNOyaZk-lNRsTd6Wpyz98zmsqXI9ucXncodFtJvbi3Lrwj5BpaKbopQsf_bh1UAcgW5TyCvjeAza4PcYuZ8_T0Ou6pQrKy"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzcyODQ5YzdmYzkwMmQzYzI0YmI1MTYwZTljEgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "component-library"
  }
  @{
    id = "7fd1dc0283124395bfbfcd4ce753d0ce"
    title = "M&D Product Flows - Polished Mobile Set v4"
    device = "DESKTOP"
    width = 2560
    height = 3568
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLu7cyv85CrXvqWy3AgDv_9lN1p_oXY7EyHBpR_W4ey1OxFJfeY6yerCikE87nRl71VmNDDF4igSzB7R0FQQVBRqLunTbyyGXIkxl46dzimUQ7sGzVo9zWuIg6v0M-tu99EolGaEFWKFF6PrDl_DE9mP8dTuwXW0M51DNvHUxqLan2JZ8AfLm1Bdg8QuncK1VHVyVNlqeKLenW4Y0jy7a2GXEzpeKAatAHurLd_PsOSEaOSBiy9HqLoe3EU"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzcyODRiNzc1ZDgwNThmZDQzZWFlMWU1OWZjEgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "mobile-flow"
  }
  @{
    id = "8d7449b2407243bd8f827d38bf4f509c"
    title = "M&D Honey Messages - Polished Set v2"
    device = "DESKTOP"
    width = 2560
    height = 3568
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLsxtpq_ejR0Qr92im6RtgwDrL0sZlsYxwV40A6sF-YCHQbnH5wCaQKFqvdsup5NzaJ4ynkvsXR0QqRdMTNTh4leSrHQzxPyM_pwiXqehSEtBNQJDb4GIu3pxQt7imGUMEM8ot8MMlr28PtR9YZz2vf112GKIZVtUDNxBgucu7jR2UV7P8N4HoZ-UNg2kE3vlIMuINkrgyQRPnr8UYXZGa3CmC4jd53ab5xTZeb30Vy1jm7ZCSvQn2pBYIWz"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzcyODRkZDYyODcwODlhZjdiNDM4MTYzY2YwEgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "web-desktop-flow"
  }
  @{
    id = "339cebfa1f904aaca1cd353b15e1858a"
    title = "M&D Component Library - Honey Core Components"
    device = "DESKTOP"
    width = 2560
    height = 3674
    screenshot = "https://lh3.googleusercontent.com/aida/AP1WRLstOMzN-llFvw5IzPQLbHoffU9nUsllvzUqsRAWFj7irjETUTKQG09j3ikZOaq_fuqi5_OFQ2SS4Ew42PJIJamZ3p1hqOn_q1Tg1fJryoUUo6GW2pvVI-pPyQpti4PxdnuGgL_2_e8Jy8JjOY8L0NQispFPL0Z-ti8sn4qVbkuydwHZeXzlF8fmKhD0xcAe7VF01KS9eYbfAUoJE8jc3tFt7HfdsqeITSnHVWP1UB4YpuFp0razqvxYlGWu"
    source = "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwMDY1MzcyODRmYTdkOTcwMzMyZmE2ZjkxMWZjOTllEgsSBxCC073tpAEYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzI3NTk2MTEwMDYyMjI5MDM0OA&filename=&opi=89354086"
    mime = "text/html"
    category = "component-library"
  }
)

function Get-SourceExtension {
  param([string]$Mime)
  switch -Regex ($Mime) {
    "markdown" { return ".md" }
    "svg" { return ".svg" }
    default { return ".html" }
  }
}

function Normalize-LocalDesignText {
  param([string]$Path)
  if ($Path -notmatch "\.(html|md|svg)$") {
    return
  }
  $text = Get-Content -Raw -LiteralPath $Path
  $oldNeutralAlias = "Neutral / " + "Admin" + " Neutral"
  $oldAdminAlias = "Admin" + " Neutral"
  $oldNeutralSlashAlias = "Neutral/" + "Admin"
  $oldAdminSlashAlias = "Admin/" + "Neutral"
  $oldNeutralToken = "neutral-" + "admin"
  $oldTextToken = "admin-" + "neutral"
  $oldAdminPrefix = "\b" + "admin" + "-"
  $text = $text.Replace($oldNeutralAlias, "Neutral")
  $text = $text.Replace($oldAdminAlias, "Neutral")
  $text = $text.Replace($oldNeutralSlashAlias, "Neutral / true admin")
  $text = $text.Replace($oldAdminSlashAlias, "Neutral")
  $text = $text.Replace($oldNeutralToken, "neutral-primary")
  $text = $text.Replace($oldTextToken, "neutral-text-primary")
  $text = $text -replace $oldAdminPrefix, "neutral-"
  Set-Content -LiteralPath $Path -Value $text -NoNewline
}

function Save-RemoteFile {
  param(
    [string]$Url,
    [string]$Path
  )
  if ([string]::IsNullOrWhiteSpace($Url)) {
    return $false
  }
  $downloadUrl = $Url
  if ($downloadUrl -match "^https://lh3\.googleusercontent\.com/" -and $downloadUrl -notmatch "=[^/?&]+$") {
    $downloadUrl = "$downloadUrl=s0"
  }
  Invoke-WebRequest -Uri $downloadUrl -OutFile $Path -TimeoutSec 90
  return $true
}

$records = @()
foreach ($screen in $screens) {
  Write-Host "Syncing $($screen.id) :: $($screen.title)"

  $screenPath = $null
  $webScreenPath = $null
  if (-not [string]::IsNullOrWhiteSpace($screen.screenshot)) {
    $screenPath = Join-Path $screenDir "$($screen.id).png"
    Save-RemoteFile -Url $screen.screenshot -Path $screenPath | Out-Null
    $webScreenPath = Join-Path $webScreenDir "$($screen.id).png"
    Copy-Item -LiteralPath $screenPath -Destination $webScreenPath -Force
  }

  $sourcePath = $null
  $webSourcePath = $null
  if (-not [string]::IsNullOrWhiteSpace($screen.source)) {
    $ext = Get-SourceExtension -Mime $screen.mime
    $sourcePath = Join-Path $sourceDir "$($screen.id)$ext"
    Save-RemoteFile -Url $screen.source -Path $sourcePath | Out-Null
    Normalize-LocalDesignText -Path $sourcePath
    $webSourcePath = Join-Path $webSourceDir "$($screen.id)$ext"
    Copy-Item -LiteralPath $sourcePath -Destination $webSourcePath -Force
  }

  $records += [pscustomobject]@{
    id = $screen.id
    title = $screen.title
    deviceType = $screen.device
    width = $screen.width
    height = $screen.height
    category = $screen.category
    sourceScreen = "projects/$ProjectId/screens/$($screen.id)"
    screenshot = if ($screenPath) { "screens/$($screen.id).png" } else { $null }
    webScreenshot = if ($webScreenPath) { "assets/stitch-remote/screens/$($screen.id).png" } else { $null }
    source = if ($sourcePath) { "sources/$([System.IO.Path]::GetFileName($sourcePath))" } else { $null }
    webSource = if ($webSourcePath) { "assets/stitch-remote/sources/$([System.IO.Path]::GetFileName($webSourcePath))" } else { $null }
    mimeType = if ($screen.mime) { $screen.mime } else { $null }
  }
}

$project = [pscustomobject]@{
  projectId = $ProjectId
  projectName = "projects/$ProjectId"
  title = "Kitty Circle Social"
  source = "Stitch"
  syncedAt = (Get-Date).ToString("o")
  note = "Local source of truth downloaded from Stitch list_screens/get_project. Screenshot URLs are requested with =s0 so local PNGs keep the original Stitch canvas size instead of the default 512px thumbnail. Project-level designTheme metadata is not the current design authority; current work should follow the V2 neutral-fixed design document and the theme/component screens in this asset set."
  screens = $records
}

$metadataPath = Join-Path $assetRoot "metadata.json"
$project | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $metadataPath -Encoding UTF8
Copy-Item -LiteralPath $metadataPath -Destination (Join-Path $stitchRoot "metadata.json") -Force
Copy-Item -LiteralPath $metadataPath -Destination (Join-Path $webAssetRoot "metadata.json") -Force

Write-Host "Synced $($records.Count) Stitch remote records into $assetRoot and $webAssetRoot"
