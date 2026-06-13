<p align="center">
  <a href="https://github.com/DeveloperKubilay/Safra/blob/assets/readme.md">
    <img src="https://cdnjs.cloudflare.com/ajax/libs/flag-icon-css/3.5.0/flags/4x3/gb.svg" alt="English" width="40">
  </a>
  &nbsp;&nbsp;|&nbsp;&nbsp;
  <a href="https://github.com/DeveloperKubilay/Safra/blob/assets/readme_tr.md">
    <img src="https://cdnjs.cloudflare.com/ajax/libs/flag-icon-css/3.5.0/flags/4x3/tr.svg" alt="Turkce" width="40">
  </a>
</p>

<p align="center">If you want to read the README, you can click.</p>
<p align="center">Readme okumak isterseniz tiklayabilirsiniz.</p>

---

## Encrypted connection

Starting with this release, all traffic between players is now end-to-end encrypted. Previously the data tunnel was sent in plaintext, which meant someone on the same network (for example a person on the same wifi) could capture the packets and read them. That is no longer possible.

How it works, in short:

- When the connection is set up, both sides generate a temporary X25519 key pair unique to that session, and the keys are exchanged during the handshake. There is no server holding the keys in the middle, so no one can step in between.
- The data is encrypted with ChaCha20-Poly1305. This gives both privacy (nobody can read it) and integrity; if someone tampers with a packet in transit, the connection drops instantly.
- There is no extra latency or additional handshake round, the keys ride along on the existing connection-open packets.

It is enabled by default. If you want to test, you can turn it off with `-Dsafra.p2p.encryption=false`.

---

## Sifreli baglanti

Bu surumle birlikte oyuncular arasindaki tum trafik artik uctan uca sifreleniyor. Onceden veri tuneli duz metin gidiyordu, yani ayni agdaki biri (ornegin ayni wifi'daki birisi) paketleri yakalayip icine bakabiliyordu. Artik bu mumkun degil.

Kisaca nasil calisiyor:

- Baglanti kurulurken iki taraf da o oturuma ozel gecici bir X25519 anahtar cifti uretiyor ve anahtarlar el sikisma sirasinda degis tokus ediliyor. Ortada anahtari tutan bir sunucu yok, kimse araya giremiyor.
- Veri ChaCha20-Poly1305 ile sifreleniyor. Bu hem gizlilik (kimse okuyamaz) hem de butunluk sagliyor; biri paketi yolda kurcalarsa baglanti aninda kopuyor.
- Fazladan gecikme ya da ekstra el sikisma turu yok, anahtarlar zaten var olan baglanti acma paketlerine biniyor.

Varsayilan olarak acik geliyor. Test etmek isterseniz `-Dsafra.p2p.encryption=false` ile kapatabilirsiniz.
