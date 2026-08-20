package com.suarakita.api;

public class ApiConfig {

    // Emulator Android Studio: 10.0.2.2 adalah alias ke "localhost" mesin host
    // (komputer kamu) dari dalam emulator.
    //
    // HP fisik (1 jaringan WiFi/LAN yang sama dengan PC yang menjalankan XAMPP):
    // ganti ke IP LAN komputer itu, contoh "http://192.168.1.10/suarakita-api/"
    // (cek dengan `ipconfig` di PC, cari "IPv4 Address").
    //
    // Path setelah host ("suarakita-api/") harus sama dengan nama folder backend
    // di htdocs -- lihat backend-export/README.md.
    public static final String BASE_URL = "http://192.168.100.130/suarakita-api/";
}
