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

## Sifreli baglanti

Bu surumle birlikte oyuncular arasindaki tum trafik artik uctan uca sifreleniyor. Onceden veri tuneli duz metin gidiyordu, yani ayni agdaki biri (ornegin ayni wifi'daki birisi) paketleri yakalayip icine bakabiliyordu. Artik bu mumkun degil.

Kisaca nasil calisiyor:

- Baglanti kurulurken iki taraf da o oturuma ozel gecici bir X25519 anahtar cifti uretiyor ve anahtarlar el sikisma sirasinda degis tokus ediliyor. Ortada anahtari tutan bir sunucu yok, kimse araya giremiyor.
- Veri ChaCha20-Poly1305 ile sifreleniyor. Bu hem gizlilik (kimse okuyamaz) hem de butunluk sagliyor; biri paketi yolda kurcalarsa baglanti aninda kopuyor.
- Fazladan gecikme ya da ekstra el sikisma turu yok, anahtarlar zaten var olan baglanti acma paketlerine biniyor.

Varsayilan olarak acik geliyor. Test etmek isterseniz `-Dsafra.p2p.encryption=false` ile kapatabilirsiniz.
