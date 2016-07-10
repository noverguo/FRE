jarsigner -verbose -digestalg SHA1 -sigalg MD5withRSA -keystore noverguo.jks -storepass noverguo2015.com -signedjar fre_1.apk fre_e_f_r.apk nv
zipalign.exe -v 4 fre_1.apk fre_1.2.5.apk
del fre_1.apk