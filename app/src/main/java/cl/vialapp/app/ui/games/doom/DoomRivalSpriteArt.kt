package cl.vialapp.app.ui.games.doom

// GUÍA DEL ARCHIVO:
// Sprites de los enemigos humanos. Ojo con halos blancos y poses de disparo permanente.

import java.util.Base64
import java.util.zip.Inflater

/**
 * Five original human enemy sprites for E1M5. Each 64x64 sprite is stored as a
 * tiny indexed palette plus zlib-compressed pixel indices. No runtime drawing,
 * network download, or asset bitmap decoding is needed.
 *
 * Layout: [palette count: 1 byte][RGB palette: N*3 bytes][zlib indices: 4096 bytes].
 * Index zero represents transparent background.
 */
internal object DoomRivalSpriteArt {
    private val packed = arrayOf(
        // SABOTEUR
        "TwAAAAAABwEAAAEBAgICAgQEBAUEBgUGEgcGCAgJEAsLEQsOIAwGBA0NEQ4KBhEUIxMPEhQOBRQVHRUTEBgYIBkaJxkeMx4fLiERCCEiLiMlMiQXEiQeGCQnOycrQCglISgrNywuQS0uNi4bEy4oGC8yQy80RzI3SjY7TjkfFjorHTw4MTxBVT4+PkFGWUZOYkwqF1NZalYuH1lGK11of2E4HmFXJ2dugWhncGpzhG5kK3lALH5ydIAkIIBNLIdtJ4pYOZdZNpeKd5yNG6RaNLOjK7VoPLlyR8SFXct3SM+sPdnEFduKWOWTYfbhFXjazVdrc6JIFI0ZjS98AAGFdqM8BRICNCMIgag8lUH8//9mSU3NJLP7haZqq7Y/UV11bjf3nnPv6bu7//ka4tNOe/SYAryqU63hrCCofhVP28EfKBbI9nv6jrfD9ymK5TaiKOLDludTJM1tn2i6XYDubDb5i5Ysh+m2ws8C0Qpmq8CetPv/2cGJrpF2eiZa1k9L86oqrinTsn6Ek1+r/OC0O58ZPR+caxFFVhu0k5QWdgi0vDiILe4/e3ZvGQYih3nG30oJFT5arzGxdJ9AdMCw400Cm42weWyeuaUKTczJ5pgWzAdJhr0Iwqvw2JhFw4VfndbT0sVoCnNvx+FohOPT5jJ8WHvpOz93Smkxl26llZ4jbdVDUN7yXVDWi/sso+duokXVOTigVLE7f1IWUmRLx8eNfUrz4hJZD0j5f1pYVRWdTudrVeVpUaQWUht8mGtFVVT1uhZnx8nzQBSRCHAv5sW1XkWe2sSEWYlB2kXBS4R9vhbFOXUYwiVmVlBEPRS85U60cx7YzEaQkreJE6VoKrKyI0EwDPHqhd4xE2c2YhN6zsrkzU3evPAlrJWIrD8hzH6U5S3x4jA+Zsj6e3j1/DDOfrzBOA4NHyD3niSLY9/34ePm5bW+AWoHk7LyVmaXSyYN3Po/ShERb9X4262Ogdfd41YmqB2MICZWUodwMYrJEvG+h4jvjMHgXnSdvqzMuwOA7gBoXt3SgwFnqDpvQAV1/o04YwcNRVf5p/prD2lEPKnvdhDqPIuTqgn3e6OP5hw4uK/P1/n+t6ViwL0P0TIwNXb+fmfKZG+85Mzd3vdlJOMA6iP3pgL6dz2WU+DODyGKh+pudzV19e34w4PxsgHDOGZR1GOEoWf+9H0dklfM8BJvEPCUd4k9E/wkXYcSdC+8fEfwrLIXX0Kd/FUM+uMC3gih+Kc4NLlfnOvVJfRCs/nwBDA8QZX8TQaSUw3IN2bQdKtC+Lr8Pa+mQNHlLdW0/XeW+na75qafs3Rdy4AbN5YOryvcFvQ+R6FsKCrbePyRqiIrMvkZb6moikI25h6tqqrCfmZ7SIOtoTemL67qpg6+On6cM0298ROAlQGQ11/MwpBVOcCSDe1DhzV3Jkt/IQsJWBlCtSH+GxBqttBfso2zqslReMMO3CEsi6G+ZrtP0o+apc0aNn47+O4wnT8iinZwcCytyQzpWNH55Kz+2NJs23acUxQ0GII9+3A6/AMvWoz2sR2sGqSPqZ9rzL+sxuRje9XyGfRfrr8BMhSZeQ==",
        // HACKER
        "UAAAAAAAAQEBAQMBAQMCAgQDAwYCAgYFBAkGBQkHBwoDAwoJCQwLCg4MCxAFBRAJBxAMCxAODREQERUREBcFBhgWFRkICBkKCBkTERsWExwZGB0LCx0YFh0dHyQLCSQXEiQcFiQhIiYODiYeGCYjKCcmKSkhGikiHSwnJy8mHTArLTERETEiGDEoIjUrIjYuKTgvKjg0ND0UDz4yJz8tID83MkEbF0U3K0Y+PkobE0o7K0tFSE09LlAfIlRCMFcxI1hHOVkgHV9XV2AmJGFPPmVFL2gqG2sfFHRZSHcuHoVvZK1EDrqCX8Cgj89XDedwHXjazZZZd6o6GIbLdsYBFUQR1DIoMigtRAGZioBVcfr/v2bHs7u6e64ae3HWyR2s9SRv3nzfmzw9/Y9Hd1zu9ys/x03WTrd6+6e8wtrHY5ravZ/h9ExM0xTOYBM/4seUDfE0DTc69gO8NW5ZabqFAo7hDwRU177JbcP0uBU52y4+vrzmaszmmHLPi0VJ5B7mG8JU6KRhobnaLQr1x/kqK3TFLbczJjuewbjHHajhRCq+neE4PBfFR6vwV+VXf2PXIL/bvRW5/iMsNmQH+9O+p9vl1QHiz/XBI3hZDdi+dbydJrreXp0PO7FXfoTn4xEXhtz+dp2IYnWxYIj6I7vvBsveFo7+9XYiJ5M61rMsroauHriDWZI7W31/208FfkAMoBXo/lGRMnAvl4utb25XixYUylwrwrBRvXdkF4Ffk/rxdD1sxM3tdrNwmtQ00/R9V9PoMY7Av7S36X6/tfE95PdjtqspU0WYmq7rm9+fQ5Gi76kRhmLleufJdpNkp4KmsaSgCOy48I17BsDtNExDq7y5nt6PV0IkLB1OQT4vGJb+to3xtdELt6Flc/X9xkxm+70unk6DMU0czrv697svLf/I54qMm7h+Mrnz1wmMovP5iBDE/GvD3uqiSIx9X1O0ZN6zudOJVDDYSMfvQ6jwGpMWzKywLmimb858v2LD9VntzlsI8pWXZj9M31O95eZJ7msJrXOQnxKDN5QMLFA0TkABW7uaJEmeCInbg/qpaV8sVOoo1T+XGrD50pRw88vF1NZJp5/k426NwHGU6m+uJbqhp9uN1UjyPJlpybSY5Q68P0ollOShX+Yk2bfhxcONk8TUFN8nrrerDnmUDMBaitISymJ6tHv4VFNg0U8nsI73kEeLPlohWZrQucX5vJi6vtYiIH+yMNQEKrSo5qw6Od5zm5/hWIUrWJtfD9yfWKk4bbyfz5fd4Q1rUjIjt7o8enxBmzFyDNUfDr3ayJCdpWOswAidvxcBXbXfLKItAc9QJRVE0RJH3T5MmFoNa7drOGNEnjFqU0YQZxFfQtz9ffmnkrQ0QBAHYF5uGABEWQZKqPqrUAE+lEGcBQA0S/wcRHEWqx1UA2DAdRxvJa+ywOExyC/hVEDyquhH2HGyg6pmgVp5wjpLwwmyePjAEZb5Ic934hgwsONL/PJuoLqS0F8ugReocuao/0iuyYYDvZABcg0yUZTxXuaM/mR9a/4KN7BwkPmul0XPWWR8WI5RhhfF0gKVr42GsOwy8LH8Ey4D4MUGKo4NX2WGD7z5h+EYry6dyDFQ/W+BlxcvANLwY0GMkV8DoH5+f//8CAxjLi8/r/qC5AFjDiREvqZGgboEfxumI6nqHKyQO3BoBJGnNv4KGsmGBxhU/ygDqLL0pVxLzBz6JyPWP74MHA8Mm5/vBIziDSBTQ0S+ozJDRjaaX85zLjXHaxrx/MotM8/Z5he3qh1y7ecJVUaKENyF96bb/DqjopmZn1xypYCi/35vHoSvBeVnrmvmlzxDsWAI8UsefzmtVgwV3f8eGijlu1zDwf/7rYBTI3ZEdx96hf9H4zehP8wt",
        // RIVAL_GUARD
        "UAAAAAAAAwEBAQIAAAICAgIEBQQDAwUFBQYJCwcGBwkHBgkICAoEAQoJCQsKCgwKCw0MDQ4KCQ4OEBAODhEPEBIRERUSExYPDRYVFxcUFBgJAxgVFhoWFhoXGBsYGRsZGh4aGx8SDR8bHCAcHSEdHiIfICMeICMhIiQgISUUDSUgISUhIyciIycjJSkkJiklJyomKCorNCwnKS0pKy4oKS4pKy8WCy8lIzAsLTIuMDkzNDsYCUE5OkMoG0lDSFwvGGtMP21kaW8VDW87HYMsAotGIpdVMaJ3X6RmQ7hKEbiEZsRqM86afNl5NuhSAvtmAnjazZdpd6JYEIaDS0BFhYhgABWURQmKBMHIHgRxweX//5q+Sc+c7qRnAvHDnCnPEY/eh1tV960qvLv7HxvWe4Chm+k6qz0Hu2hyI15mdS2ITqfX6m18m5efN4ck2dRu43FOkMbKSu63b+NLBM7bzlpfDrFb8FH8oPQ63ZHLQI1bMuCmfhr7DT/keze5P0rT7HTcZvtteBPfXKXZ8XTep/tv85uNm2y6/jY7HsOXl1QpfYuuEKvker24nrI9Z37oxkqSuMVx2Imi/uF6TRzvYZuGgaP0/cOB+etXFM/BkWC3i7zwejmsAqc5quE4TnaUpIfW63UYviPYHN6MgAWuO3t5GVLS0lpOeGk2mQSO92aUysKt+leiDaJoFwWDzmKyMAlVFvmBYa8527bX4OXwniQ5QvnfeTsKPOc16Pc4mqpC9zWSkybSGKXInqKMOnyrXq3jX+zPOn68jcOXWq1Tgkbp/vWF4jheaCFADPt9D8NzlPzoZ+fjOdv6fhjH+/MpO6bM2HYoKM6O+yyGmnk9Iwb4+Xg8ATuDD7EfK48DqgX5aZoWEHIzzs4APWYZcPd8zvYhNHrgbBJthXHoNvPVE54BCvZK9ynwP0t999DTnTXMwFV4FTO5/Aq4CeD91h81lXgbVpILM3EceAUyst3nyxhm/DjdxqvmKjkkTOfJCS8bcu09juIY3FcpoP8qM2IYJjlcLtckCqLgcqjpAgb4OA0rxSo/BPQVWLJzdtHl6jJupekCXTAFG2/wmu7Pb/wpOgH+cgird1XFLYjfKa9vNbTbHb0dmB6gHKPn7wyxUnI5pLs3LDDN9TO41WvY/Eb7gdz36IEOzntmCy4H0AuKD011/agk7+kD5r6/Xw5MYb679tbk/ernCRyYd3rTLB4/KlisgNRKVWaTJCsIjE8GukcrRfEqS9EChopPLFkrle6JUqnWoYVho/D2oqYP65ymyfJsJvblntzvq/p8CBd9YhENlcS0pamLrCCzfZKi5wvD0uhyMR7h50KLNU1Lm4k0jsANShxPdcuaFo2g2mpjC8s09LnYhyEI5wVZNUxrPSg6w2FMspamaQgsBlXKaFcGM8AyxaIZgEjVsteWOeUosCOEUTitWsZwael4Mf3JY2k6s2b84L3NQ11hyGq07UWBVsgDjOT0xYCdsuhPzZVJUWUJ2wm8NVrk/Lu8F0UORXX/lmyVIDDcADEZRXisa9hBYPPwL2erpIwtFsZcLsLD5NK0bYPEkV8ZxYk6NVdltkD8dQKfzZe61G79qjgIJsYtnMLRfAlWu084MZZlHPltLUxNDZUiaTp/f5gez8k23saQ375s0MJsbuoykd8EUJ5TdXE4+LAU5VlR13gWLVA80/5QFjnqg6etp6k0ZykOy5fukybRBMtTHzIFU6pKc6At5MUPUYJk6TxJYx/bFUpRqr2e8UieAw1eHopzHv2UqBY7NaYEhuVWMFRvIAjBfl5XJvoYQuD56S93OZlvI3/oBIJxeTbt5uoH5ifS5B/CxMEB6pbQyPMAn4Mynf35eEcKA2k91z1P+jKFFc72gmBJ/qFqos/yY80BdfmlBsq8rhuGNvzsZmMo063h7K0nf/lXrNwiuiRJfD4+0JIJBELxLkXiRcfIf2c/AJzI2NY=",
        // CORRUPT_TECH
        "UAAAAAABBwEBAQMCAgUEAwcFBAgHBwwIBgwKCA0LDQ8MCxAQEBIMCBQTFhUQDBgVExsRCRwYFxwYGx8aGyAUDCAdHiEXDiMeHiQgICYjKSgjGikcDiwmKDAqLTEqJTIdEjQkFjQsKTQtMDcvMDgxMTkzNTwyKD01ND4nFEA3OEIIBUI8QEM7OkQ2LEUpGEc9Pkg8NUpAOkpAQk1DRk4yHU5ENVBISVVALVVISFhLQ1hNUFlVYF1PSGBFMGBTUmNXWGRUOmhYSWhdXmtGLGwtH3BfUHVoV3xJKnxxcId3UpBaNp+SgLt3RsWYVc+zRO3WQHjazZfRdqJIEIZDRKMiCqggBogGIcKIMEIvYkfppHECg4jv/zTL5Mwms1dpvJq+4nDOR1dX1f9Xc3PzNy9+POCvp9vWIU2U4f3dlbib5sl8+rN8pa/iZ2D24AjT8+XcuwanefU5y+S71zNqXMNTYvhszNdSVxxcFf5gPh+41jZPjwpzTfiDNAU56M/SGd286vzHPD2f87RIlKvib/F5XpyL9Jxe00KKNWDcil8/8FJ9nk/KEqjHY5GDFEi1ccF5KC75dA3cvMqAC2riI4RDpbwAuLbGSeJapXVbB++uEHrT8kuyCmdHttcHl8OwDj99Pp0yE1xKmdOANR1WXzJrdEBnXZV9HyiXUvICQZ1WJ9maAjkvH6sVmb9437ONVXIplOmAalAU0+l+jd95GCMYydal3JxO6kQDyf3CCRGq3iLjayVyMYwwcrrppYhO0MToebXCMAxXK0/URtSXvAoR9n2TLy6pKOsw0DhjIDEzC4z7TerrNDa9LNZksc/nZTrkVrpuB+bMdYvinPRIisDE0KziNw1VUXoTYSQOhT7L50WR50Q17MEYZxlGmZHmsmnI926bp6ikchGrRaRaJ0b4LcogOBcj/bHPz6yU7bdnPEtogkM7jCHM1Ep7gv4wYW7zUhHF2x6xB/c03/Ow4Oagp6oDTsxLcCurAvkQaQqqFspNttXlmIathtmqQXdqWDhNNbtTtaN0eoL5pHFxlrTryK+DhQ7HtsdzXjrcHxS2LAu2Ds+F345tiffi9ni7dnm+LNNWDZyaz49Hdtp9e+TM4eNE45IZRdWxbYk7fuvZ5lvXjpbQi7WubtQZ4JI7Pn5TtdOJi2If+rH/CO1hjexLYAt4VT5lIkKe78FId6Ia9kW7Fc8uDK/SLvRMM8SxjZFIfukA24PLBLE/DKFjy2YlJyfCyw5x843BQeEgghqC3qRjmD6ODdvhyOvHSm0tgLaNA6/L2LAyvkrPNnkJqMYkQPEqilYqJes+zE7Vwn1yXkO/kAz7Q3qkLd/xDJMPAMrP3vksbN40nRBXj+jl5Ym4hJS8f339Eb9lWvU8DByM8MtL/g+xBuie/325ftptfjlOQzXtGL8m6ZY8gar39H1th4v3He+q9kN4d9iOyPO3eXp63ATy+4kpznR2eLddT0hFyCyj5eMycn4bZmOyiFDwfS2T7j8MNo7tBNrvhNGqHcfOZmMQtjBjOPZyEXj/7UdrDoq82PMJ7yC9cKktglDnfp+3ITs7G+53ASHPOL4XQVv7mBfMwvbiPdqPSHmIsafJH/1GT8wY48Ah7OA7UZZ1z+lTn/wu0mV5SngJb+twr4qaSH3Os4Wq7t+wR9aBd6YHceUcHy/6suzEcCHIRAVsKpbCj5bap+M2BE2fDCRLIhkDDSv/mQKF/TPWJtMeg2RbaejrOUaDJEmAO2v9L6S5C0CS5+n8ywhoxQWuNeb/nLituTueg0NyALOrfkRolqdavCRJ487NX7f+BbLJv+w=",
        // ADMINISTRATOR
        "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"
    )

    private val cached: Array<IntArray> by lazy { packed.map(::decode).toTypedArray() }
    // Five distinct lowered-weapon idle poses. Indexed compression adds only ~8 KB.
    private val idlePacked = arrayOf(
        // saboteur
        "Lvf39+7orOXQKOXMAe6uX++eY+ebXN2NVqOTfq+UBIGHi8h2R6htK6BYM3d0gn1NKVhzjk1dcTxNZ0RHRXc/JK8ZJWE0Hjw4XUA4MkElGDI/UzA7TS03SCwyQy8rQywfKyUtPyIoNSIiMh0bKRcTGBQKCQkJDAYFBQMEAwMCAgEBAQQAAQAAAQAAAHja3ZfRlqMoEIa1x6AoUYTQKGhUFDWY4f0fbzEzneyleLEXWyc5Ro+fVJV/VZEg+F/bQ9e3ej2N9/pWlmHQhefwte9r8v393Z9cfuqnumK/e/N1lteT88DabSnO8Hrrn/2Dc26XUzl8zgtjFmt+8g3kCF04j0XOz/EzzsEliX9fyCl80wwhAHCMz74/NqMkuIDbOVx3ghmcxPUZ+YXrLR2rJ+bP5+S/fmjmCBa4o1OObZdiXwEOY/UIYKSo1ontIphaL9x2VVUFEKZTN+ppimBUffllrqpGF0EQLZWu7M5PPmWfBWHTTTtvKnd0/NB58Iur+aoZMfwVRbSjgeOnER5P/rzzg+YZjKLrYqIoS7vKeIQ/dlPXmbRwfDS6byG6yqOJTXZ9duOaksKhdRalZK0qjwRsrmqSuSdFVhSQh0VBbI6FR/wIxPnW0sIZ1DufgwSHx6WfgyDmOE1d6DAI05QzBPLjPEYovgDx4jFPo/QJAhB79B2WgBigNN2fcI2iNL+AxKeH5TmKk8QWWQZhkUELAMi9eih2DwDgVkDHw8zxSe7XRARzD0ACO+ed+yBGm18D4AK7HF4QX00qYqcGP/yGMXdJBHl9IyZJEu8O/FidAy5tvdaXC8i9+VbqGcX5xdSZDhJ89+2fA5XrledX1K9FjJ6NZwv/WihpGWY26fvQoKX065/B0pKyHNSwzKuu67ksPSewoqUzOYzr5hwYypL4BXB/8ZRO4bT1de94rwAy+eLJMAVG9Yt1vx9ek3skr/X1Hord9pPFhzeKfMGSNPu+wSoXTJaVPuolJI9BTe0fKSiaQa/6NZreEGLNn7n9mKS8Ih9+kpTATA1/tTAMEn7B4wkwA20kvau/I3O9y0HS9rj2W0mkvLfNz17A+S/l4CE+d3vTyPGH389beXz98b4v2Dzee5GWOn8O87CVkjTt+KP4u+ObdzQHSp8QVbbqLfhV3Qmh9GgB1HfdEKXa9/3LfVBa6cP+N6QkRKr6nU7Vugv0aAE9FGezGNSnlhupVj4f3n8IZ7P4yHW1gvNZiIP/IVbGUHJln35jhOCM42uCD0nYCuaMf+bVtjvEubt4aIYJjhln/yo3LYQV+JozfmiEG/N4fT68MUthzGaMZxP/L+wf0BxWUg==",
        // hacker
        "MPb29t7Z09myjq+kmtONT9RtIop7bstWEIJWNW9XPWNVSVtIN581IYcvLVw/KnIuKEw+N0w1JUQ7PTczPEMxI0AsIDksJo8iJ3omKm8iKFseIU8UGTgnHjUjGi0jIjYaFikcFzcPFiIeKx0XGBwTEhoPChAKCgwIBwsEBAUDBAQDAgICAwIBAQMAAAAAAgAAAHja3VfblqMqEFWj8RpJI+KoBAKtYlBs/v/rTqV7Ts95JP1wHqZWkhWzsuu6qSqC4K+Wply3Ofwpeio/6roex+mH+HIfAF/3I/mZecH6+lPYjxSUlo2ggNXDoH6SO1LOwzDU/SFtvb8M1020pwOrNzxP/KF+ULrGlqx3W54n22ZfdqBpml2Mj2W3tUb4cK/iDXkXrr+egvyUnOjDvIqPDufGjZ4TipPDji/Hv41s+LXiM34sdFP2ZffdoVhNjcyv0lldvkS8VN4R5kwpO1opuVleit7FKbsMKN9+baPNN+vUx/oKfn6YoR6HJM8uB7vn431Z0AsOzJMen4fGJVkGISR3p7LEaF941b4Zktb1MObHORvPTC1LQJM68qQ9righYVgPLH8EAcsGpq7BkY+bH95WyF0ul1M/KpwFwbns8yE4B2ieveAhwlj15EJ6ZT7xH3WSB+eMD8yvDWK5jHUPYrnOQDZoAOcsO4bR6wSGejZDX19H5txapmUZ3fshyDI7jF4l5EXLoOcw9eiaT4ld3QPejczrCFWyrfuBPZT+gjcELAdAJKWYjwPiaf9ZfNGU8ROvGeg7Z1apwScBXdHdR8jeOFVRFAFeKOj/aeHgRx8K8qqz4EDP1ogQcKDTvxhbsosd+n7xwFPUSdWDAt2UT/OpHWazXOpj8LO/aSzlOCjmSJMSUOCaSWjIH5Di4YFfb1RqpvqeCfKET+rjfZq7C9uuivnwR0grHOvtOdmeeMmOd0P0dIy9H4ECq8W8XBOQa0cI0Wol4l0bmTjPEbDKaTkDfLtDBSZXgkJRNmty9oOHQoj9idedmBs3NYsiQKbUJZ5NUHIB0V/zfLHOSrMI02koRnz3HIGTnGe3LNeH5C3GC4V2RMw7iUkaeJ5gAjmQN1wVFeLauEl3yfHehE3sPfvaeUZFUVDEnWzE5PJkFuRd+64xGlVPeIHRJndBxO3Ad0irmz3xGLAS3tUc6lIIU+ED3JCN7xii8yZvT/vAhrXpaFG00kx72TR+DCry4vlCwDgjZ/0MhTswbvwcoBxM53kF5oN45RwqUVTcfwJKiotH/mn++YRayIfB3vCQS2eK4kY/J0a5zuhWIO4/gXfwFyGKv4INpYTvmPsvYLcbxhQ+viZmSgF9o3TzhQsKM/CGEf2a2Du9obbFyHuBjHjVFlXb/r43OD5RVGHkvUDEHLVA/PZ3wkIJzhScct9rCJ+hXO3M/+XKQfG8+tc/XjDiCNP1Tz+hVVW0N89bTEjXNI5ty7+fK3wzceQ8F6DAjmmarvb7qNjHTt4qfXrzax9hJaIw6LrvdBloniwqmyjya2BdGp9O8Z+F18BFJu3e4NfYh8MOoo/hv98b916CgMooTn226NDsxmzmP6b2fRP7uu27fWkL/1/kH6lvdk0=",
        // rival_guard
        "MPb29su6p9eZVtiJQLKFW9xzH+pgBLVqNYRsXGliXtRTCbQ9A4FEJllOS107KkhAP0E9QEE3NDk1ODkxLzMuL4EeBz0jFzErKjEmJS0nJywlJCoiISYiIiQfHiIcHCAaGR0ZGB4VFBsYGBoVFRgTExIQERIMCgoICQwFAwYFBQQDAwIDAwEBAQIAAAAAAgAAAHja3VfblqsoENVoApKIaCASUdB4v+Dh/79usPusnn5rOk+zpvISWGtDVe2qXeh5/2MjGy8EM+/CJ0HvJ9/3pzfxbcXv+Ha7sffgp05Vdwu/8bfg4tTVamScp/flHbxUphzZfczzfG/fwPvJ6xmbdU10/hYDfjIaGCZxkMT6rQQsMEnCIPeC+L38mz2BgbVofgeNzE7yGAbwFd9/j35cL7dHhvIxOgf58lv+NoJnAMEUnfN5XSocXX9VwhpHUfQAISBnlsfzgsH5ik/uXUcsPEIgADdD04TUIwi8CPnOeUcHPvLCwPPSyEQp8AIQRa5F4FNsD0Dc2Fs9MKho87wLQFHqfj3CWUQXENrV3mzgwBNK3QKw2T6RCGdyBZeLB8BWAeCF1z1DbhzcU3I+Rxk14w1eANgRmwAIbwVCwsmBKQnjhGG6jQ94uV5nhCw+epQ2KicK1zgMYbxWhKUYIaSZWjDCOSF6c2Igh2EYx/m0ktHiI2PvR5jolz3Uib0YhkEQmmav0B7H8WOe8+v1Zr2CMHI5wHofhgFcXusQ5cCad7X43UoBdJKh+IgfwrwaRs5vAFy9q5Xg3boFRyfVORJgLR+rlZjL8fdygTsMHPHeYqM+jojzsZV3c7mEFxgbGxL841b/S5wnEMZhEK+9TOeit/6bIyhHGfa1zpPkoMHIIuVySG6P3d7vKqNPQjKdf7AgJO7Zg46zzT7cHfuPsmx65TZhQTDxyaiqYVkcOl+vpURluUJbhSEUBWGmOZQcbq5jtxYYZ30yx3CG+0JoMQ02GaGrfiy0xJR2vd72ycwFRe1wKmJb0q74Z/mks2KlarqxYoxMTCAe5Imr/qlXXWpKM87re2fIuqoKV7Yl3Qdf34gsyyjVtUDbMMBOt2hZ3cfPIDi1VmAymtVGEYydZO4jqG4KXljBrXWR9YNqpiQfOVs21zFYyxWdzwtX7Wmz7peyTSwVbVM4JvC5HMqXmea48KVUufJSWzVwDX/U2Fqquw897TpFKKYSCcdX1FqOKcYk6z8rRilSEbucfOpUQlvbYJZao/rzFTlRw9Os2LyTqB2iryVTxd0mv/zcGBdlOOeV5U+zn3NglCg6ScjUftLld30lDOV/jqX5uQnXTnFeqFYo8rGOml5YBmvl2L99z2upmkZ1n+PSNENdlk3TaNfu5xm9s6f6m+zToEpelAVzY+/Udb0xAy/+suf5U6c6URvdVE7F13HbuOXYf3F9mqaFibLsnca/qcW0r0wNX3ibAFlwYyan8TmtjRnnqhm+Hgtbp+1WMTVuDdwNgma9Vt8imjou613w0uGEpS8oJph/6/VS1rJsBcEZXxziV1I8Zb1+i8ho3TWylEL92EB+KzKaMZ4N3wpa1UNNM8IyOfxcvtu2rfb3r6f+qvWxabZNo//cJ/I/2FF0Fg==",
        // corrupt_tech
        "MPb29urmyNrEN96hQdiQUKCVd791PZdzPoZ4a4BuTXltZXBjXJBbKG1eVI1NJnRIIWdcV2RYU2RPP1xRTVZKRVBDQEhAQZAhFmIwHFQvHEY7OkU3MEcrHT82NzowMjovKjQqKDghFyslKSIcHB4UERIOEhcNCA0IBwcGBggDAwQEAwIDAgEBAQIAAAAAAQAAAHjazVfdkqMsEI1h/CFgRAWNoiKK/+Ly/k+3zG7NXDPZ+qq+vkjFVA403afPwdvtfxye5/0DGs4KmRS/C1+fEJrp9iHfhBcUb4f9kr2H16aueNd9hOtbNVBDO6lAjuu63t/CbyWT5jDXvq9vtS6czXRdUaT3t/I/6niK73W43y/1Dv4ehq3v+/bjrfRvgdEx8fNp8cv3+seSiKzNsedv4Q8ieEsin5Dq5+UzYZiDOs3q04/Dpf5p/w/GQ/AABYJL2+6Q5MPP4BwVCDwigM2UEK5/QdD9BC9ShPkCInBJVvjzPDzAWLjDyzEQCGcWD9ZM8PIA0ePIDmf8dvfuFKXg8QBgQwtqAUh/4cuZ9y8bqFiGyOI1hB0wVbUg5dzFsSxfqFRxHhEDSlQCHECjnAugBMZdiicSgvgXQEYD3G+GFcKVOx1GdOSnCYK7BmibQ/RsjrRwnaJTMATpehn24gZMdy9GuDZF17vmX+IRIrabrKDGX1/3nHZKq5W7skdSTBESQ5EVKjDBvYEon4V8no7t7woLn8LddP3jpo5jeuS5LjN5uvavL6kVnmUmsR0BRaNnTkZOmXbln+y7KEwhakgM8EoBCMmQQO66/02Lo1rtEeIw1tnMPuKYVKEfH84EPtdKFjT8VM/hkhZf11NIAtf8X2t1STFYuH9c20EIWUuUORN4fQ3VMuw2Zf9Wtz3M9HmwAgvXATKvYWgmy//PBGQvy/K4Yj9xlqDaal67b2cYJ1PYSim2wMS+71z+NSextd6pau3gJ5uQgbqp2G9d82+nmtS9VFKpcZxHKUSgAu7HzvkvNWkZLtmk7OnHWQs8jClP3PdvyZLiNE3tAM02h2uqhpU5u5h3nsdFsxSXVJ56lv0W+kO1l+XouoDkENugfDzt9lzuSdhWhqfQ1XlR8BmMbTerGx2TCzFtomXgpmGQZQhaPMymm5YMQtypZWmjVcDVuPifYLZ4GGIruZ6SFNKSj1oPyyygC14rmmL8pMXmfaoJKygtGuWdY7AKFxuZJX3a4AX7I+eC8Sxng3fztNNNMhCc8bxpsPz8NxslFyxv3C/BOs1o0/SS/xGc0x6gFE3fu9LPa2pKeSNY8adWume04E1TusrPrckppaKgzd9mNI31gzxjzpeghj2t9WD6lyy6UYw9KW1cyXcbU8gKlJf6azn+fEDKXPXjLEdL/7n80jsjurRXMHOdnm0q6DPl3VfDRjn2WcaYq/6OdDTLKrv5S05b6wWFWVztYzN7kpBh+8Y3vTBDPdVVNTvJd0uSzwW+fxiGobUeEkdJ7cIBXduo6m98sEyTXcJuXw+pi/xVpLULEPP9HmX3b6s4tngXCzfHYQXwOL747m32cTuPU2vzLy+z/1X8BpIjc9w=",
        // administrator
        "MPX19drQmMalg7WLaLJtQYF3cHRiWJtAJW5ENVhCPT04PjMrLNUdA68bCUchFy8nJiwlJCojIikgHyYhICUfHiIcHCAaGR0YGMoSBLsUArMSAqgSA5sSBZwMAoEMBm0KBFAIBiIRDxoWFRgUFBcTEhUREBQODQ8MDA8GBQkGBgYEBAMDAwIBAQ8AAQAAAgAAAHjazVZrj6M4EAzhbczwCBiHlw3GQDDO+P//uuvc7Gpvv4xn56TTWUqiWJS7qa4u9+XyP16Oc/0X6CvJ9xnn38Y3hT6O/PbdA45b7rvXh5c538Q/Ev/ie370PbzjK/14zu6cfTP/OPJdL7uEsfke+9mM/Ot80d634O5N6dD3XOR59M8TcAxraAz42I1cWkx/RuExMT7fquMoY+A/k3XF/0CHrO+7O8cYN4Gn0KZCUySKPZMvwjnveJoyw6dNX51EO47BWg9tP34Jrvq2Tft7eu/b6R05PJ2uZz51adoy/qXwwz3t0/s9BcSskPt0w+MB/+73ext+gbrAcZxh7Nr23qdvOLmkU5ID2n/e+17b8UkC+Dt/uKnnHy3Gx70LcZumF7/ljFnLWI0dJMr6yXWuzpkWuGHMNM/0cSo9TdwqpBMn4511fLoGbvIA/J7sea5SHxabByues7YbesZU6F5d3hYYqzMHF3rh36c3ZoGHvG8ZiI9rDiJq70Ksl0tQ0rTrWM8444el+HxM+fTkE5zRdQNr8vqSuiVt2x6yeo6DRQLzOPK0m7meRg7xn0WeB55bFFPX9T0zE+e75fW7rmvdIDxeTdCZPMfXa5AX5gd+6meL9hnz3CAIbnwc+thAfFKWgB+6fhgVhwScz/E8DALX5eM7/AZrDgsXRbENXjSy59SNn1fg4CgOg7Ztu1HFUSTzIi+pLCqaRlEUZxxo/JzACcWof0nwadjQi7IsCkmrugE84xNyXfa5hEyC2DB04+bNRo+0Lm/VXlUVmTjgOQp9mxmbBCicyCVy3au/CULoIUlD9fQqQBMEVjN/Bw6xH3uu6z+UoiXWuG6ogWqOTL9FobUFDNfZ7Hue594yN1TYlGUjdN/2/aDaltnwKlOTycB2fdfkSXLZD4VrYca2hQK2/Rfw2bwZ34siPypxkiBlVLFqwPcjq7UVPz1BZFMG5X/6muLkJEmu1T62XT/ybLZa2IRQHKPsMT+2x2YkTpw8RHpFAXQFiqLAlj56KTBGCULZJvC5Y5wofMGwGYThEVnZT1AYBq4HDw/bvgh51nl8K0MUhAMLtXWWQBkf3qABej94NHJd120/iuRGUaiggMh2DV45G8D7X+vNVM1KtvPYjcxinbgJmIstfQ34D3g76LKSqzHbth5z5j9qhJ7cRt+D/337Qv59Skuy7mbdFrFscEBmEtdY/NPh3Bhz6ufU93dRE2HmF4lk0SqLM4MdyyXugHpWSSld1v2oaro9JrXK5VbL45Flkc4RsghoNpIWRY4q2VRiMQG8gIT5g2xbNmduviY2Bk5FwDVFVZa1WE4nVHJbwEObRa6PJqhOyzTr7KIiJVnKqhYfc5cRKyRQARerVujdpr+qKhuCKakFWT9OPI9G1DUVu5QX+wVc1pC5pDWppfrYklSABdYUU3pY575VkKqsIB6p5I9gO20ayKrChFjxlwXsGt69quta/mzJVzaQQSkWe/xd3pqlqeuGiPPHnlxWMHJSNHtjjy/qneBdlVSsP1vVkYtoSKFkckjr+CVf4ikqcZO/Yikp5FrhIoca2uivCTQALepF/nr0WJalqnJhtK1/Lqdujj0XmtDF/INTScl67ELs6sSf93+TozCv1mNd/nHoupgD7jJaojAh6DO8FJTAnSfO7bdTwUPAyEjTNIJ+2oBNDSNbDtKnv7EKme+izDFC+Hb7rPzneWoNX+a3KCHsJUrDN3ysNfxv11/+BZ3Y"
    )
    private val cachedIdle: Array<IntArray> by lazy { idlePacked.map(::decode).toTypedArray() }
    fun idle(kind: DoomKind): IntArray = cachedIdle[when (kind) {
        DoomKind.SABOTEUR -> 0
        DoomKind.HACKER -> 1
        DoomKind.RIVAL_GUARD -> 2
        DoomKind.CORRUPT_TECH -> 3
        DoomKind.ADMINISTRATOR -> 4
        else -> error("Unsupported rival idle sprite: $kind")
    }]



    fun get(kind: DoomKind): IntArray = cached[when (kind) {
        DoomKind.SABOTEUR -> 0
        DoomKind.HACKER -> 1
        DoomKind.RIVAL_GUARD -> 2
        DoomKind.CORRUPT_TECH -> 3
        DoomKind.ADMINISTRATOR -> 4
        else -> error("Unsupported E1M5 enemy sprite: $kind")
    }]

    // Descomprimimos el sprite una vez; hacerlo en cada fotograma mataría el rendimiento.
    private fun decode(encoded: String): IntArray {
        val raw = Base64.getDecoder().decode(encoded)
        val count = raw[0].toInt() and 0xff
        require(count in 1..254) { "Invalid Doom rival color count" }
        val offset = 1 + count * 3
        require(raw.size > offset) { "Truncated Doom rival palette" }
        val palette = IntArray(count + 1)
        for (index in 1..count) {
            val k = 1 + (index - 1) * 3
            val r = raw[k].toInt() and 0xff
            val g = raw[k + 1].toInt() and 0xff
            val b = raw[k + 2].toInt() and 0xff
            palette[index] = 0xff000000.toInt() or (r shl 16) or (g shl 8) or b
        }
        val bytes = ByteArray(64 * 64)
        val inflater = Inflater()
        try {
            inflater.setInput(raw, offset, raw.size - offset)
            var written = 0
            while (written < bytes.size && !inflater.finished()) {
                val amount = inflater.inflate(bytes, written, bytes.size - written)
                check(amount > 0) { "Corrupt Doom rival sprite" }
                written += amount
            }
            check(written == bytes.size && inflater.finished()) { "Invalid Doom rival dimensions" }
        } finally {
            inflater.end()
        }
        val out=IntArray(bytes.size) { palette[bytes[it].toInt() and 0xff] }
        // The original AI illustrations were on white paper. The extracted idle
        // poses retained a few one-pixel white halos around dark silhouettes.
        // Clean them ONCE at decode, not inside the 30fps render loop.
        for(i in out.indices){
            val color=out[i]
            if(color==0)continue
            val r=(color ushr 16)and 255
            val g=(color ushr 8)and 255
            val b=color and 255
            val minimum=minOf(r,g,b)
            val maximum=maxOf(r,g,b)
            if(minimum<=210||maximum-minimum>=45)continue
            val x=i and 63;val y=i shr 6
            var bordersTransparency=false
            for(oy in -1..1)for(ox in -1..1){
                if(ox==0&&oy==0)continue
                val xx=x+ox;val yy=y+oy
                if(xx !in 0..63||yy !in 0..63||out[yy*64+xx]==0)
                    bordersTransparency=true
            }
            // White specks at the outline are background, internal white
            // highlights become muted metallic grey rather than glowing dots.
            out[i]=if(bordersTransparency)0 else 0xff8b9797.toInt()
        }
        return out
    }
}
