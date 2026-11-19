package cl.vialapp.app.ui.games.doom

import android.util.Base64
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

    fun get(kind: DoomKind): IntArray = cached[when (kind) {
        DoomKind.SABOTEUR -> 0
        DoomKind.HACKER -> 1
        DoomKind.RIVAL_GUARD -> 2
        DoomKind.CORRUPT_TECH -> 3
        DoomKind.ADMINISTRATOR -> 4
        else -> error("Unsupported E1M5 enemy sprite: $kind")
    }]

    private fun decode(encoded: String): IntArray {
        val raw = Base64.decode(encoded, Base64.NO_WRAP)
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
        return IntArray(bytes.size) { palette[bytes[it].toInt() and 0xff] }
    }
}
