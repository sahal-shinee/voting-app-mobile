# Suara Kita - Backend (PHP native + MySQL)

Backend ini **tidak dijalankan otomatis oleh siapa pun** — folder ini dibuat untuk kamu
download dan deploy sendiri di server lokal (XAMPP/htdocs).

> **Catatan sinkronisasi:** kalau kamu sudah pernah memindahkan (cut/copy) isi folder ini
> ke `htdocs`, ingat untuk menyalin ulang folder ini SEPENUHNYA setiap selesai satu fase
> (timpa folder lama di `htdocs`) — supaya tidak ada file lama/basi (terutama `index.php`,
> yang rute API-nya bertambah tiap fase) yang tertinggal di server kamu.

## 1. Persyaratan

- XAMPP (Apache + MySQL + PHP 8.0+)
- Folder ini disalin ke dalam `htdocs`, misal: `C:\xampp\htdocs\suarakita-api`

## 2. Setup

1. Salin seluruh folder `backend-export/` ke `htdocs/suarakita-api` (boleh ganti nama folder).
2. Jalankan Apache dan MySQL dari XAMPP Control Panel.
3. Buat database dengan mengimpor `schema.sql` (lewat phpMyAdmin > Import, atau
   `mysql -u root -p < schema.sql` dari terminal). Ini otomatis membuat database
   `suarakita`, semua tabel, dan data contoh (admin, 3 siswa, 3 kategori, kandidat).
4. Edit `config/database.php` sesuai kredensial MySQL kamu (default XAMPP: user
   `root`, password kosong — biasanya tidak perlu diubah).
5. **Jalankan `seed.php` satu kali** untuk mengisi password asli (di `schema.sql`
   password sengaja diisi placeholder yang tidak valid, supaya tidak ada hash
   tebakan yang tidak terverifikasi tersimpan di repo):
   - Lewat browser: buka `http://localhost/suarakita-api/seed.php`
   - Atau lewat CLI: `php seed.php`
   - Script ini akan menghapus dirinya sendiri setelah selesai (sekali jalan saja).
   - `seed.php` tidak butuh login, tapi lewat browser hanya bisa diakses dari
     `localhost` (komputer server itu sendiri) — request dari HP/komputer lain
     di LAN otomatis ditolak (403). Tetap disarankan menjalankannya segera
     setelah import schema, sebelum membagikan alamat server ke siswa.
6. Login awal:
   - Admin: username `admin`, password `admin123`
   - Siswa contoh: username = NIS (`2024001`, `2024002`, `2024003`), password = `NIS*` (mis. `2024001*`)
   - Semua akun punya `must_change_password=1` → wajib ganti password saat login pertama.
7. **Disarankan ganti password admin default** setelah login pertama — aplikasi
   Android akan menawarkan (tidak memaksa) lewat banner "Ganti Password" di dashboard.
8. Pastikan folder `uploads/candidates/` punya izin tulis (writable) — di situ foto
   kandidat disimpan.

### Lupa password admin?

Jalankan `reset_admin.php` (pola sama seperti `seed.php`: localhost-only, sekali
jalan, lalu menghapus diri sendiri):
- Browser (di PC server itu sendiri): `http://localhost/suarakita-api/reset_admin.php`
- Atau CLI: `php reset_admin.php`

Ini mengembalikan password admin ke `admin123` dan `must_change_password=1`.

### Database sudah pernah di-import sebelumnya? Jalankan migrasi

Kalau database kamu sudah ada isinya dari sebelum fitur riwayat kategori & hapus
siswa ditambahkan, jalankan `migrate_v2.php` SATU KALI supaya kolom yang
diperlukan (`deleted_at` di `categories` dan `users`) ditambahkan. Aman dijalankan
berkali-kali (tidak menghapus/mengubah data apa pun, hanya menambah kolom kalau
belum ada) dan tidak menghapus dirinya sendiri:
- Browser (di PC server itu sendiri): `http://localhost/suarakita-api/migrate_v2.php`
- Atau CLI: `php migrate_v2.php`

Kalau ini instalasi BARU (baru import `schema.sql` versi terbaru), kolom ini
sudah otomatis ada — tidak perlu jalankan migrasi ini.

Kalau database kamu sudah ada isinya dari sebelum fitur jadwal voting otomatis &
log aktivitas admin ditambahkan, jalankan juga `migrate_v3.php` SATU KALI (pola
sama: idempotent, tidak menghapus dirinya sendiri):
- Browser (di PC server itu sendiri): `http://localhost/suarakita-api/migrate_v3.php`
- Atau CLI: `php migrate_v3.php`

## 3. Konfigurasi Android (BASE_URL)

- Emulator Android Studio: `http://10.0.2.2/suarakita-api/`
- HP fisik (1 jaringan WiFi/LAN yang sama dengan PC): `http://<IP-LAN-PC>/suarakita-api/`
  (cek IP lewat `ipconfig`, biasanya `192.168.x.x`)

## 4. Struktur folder

```
backend-export/
├── index.php              # front controller / router
├── .htaccess              # rewrite ke index.php + blokir akses file internal
├── seed.php               # jalankan sekali, lalu menghapus diri sendiri
├── reset_admin.php        # kalau lupa password admin -- sama, sekali jalan
├── migrate_v2.php         # tambah kolom deleted_at, aman dijalankan berkali-kali
├── migrate_v3.php         # tambah jadwal voting + tabel activity_logs, aman berkali-kali
├── config/database.php    # kredensial MySQL
├── core/                  # Response, Auth, Router, Upload, MultipartParser
├── handlers/              # auth, categories, candidates, votes, students, results, admins, activity_logs
├── uploads/
│   ├── .htaccess          # matikan eksekusi PHP di folder upload
│   └── candidates/        # foto kandidat (pastikan writable)
└── schema.sql
```

## 5. Endpoint

Format response seragam: `{ "success": bool, "data": ..., "message": "..." }`.
Token dikirim di header `Authorization: Bearer <token>`.

### Auth
- `POST /auth/login` — `{identifier, password}`
- `POST /auth/change-password` — `{old_password, new_password}` (auth)
- `POST /auth/logout` (auth)

### Siswa (auth)
- `GET /categories`
- `GET /categories/{id}/candidates`
- `POST /votes` — `{category_id, candidate_id}`
- `GET /categories/{id}/results`
- `GET /me/votes`

### Admin (auth + role=admin)
- `GET /admin/categories`, `POST /admin/categories`
- `PUT /admin/categories/{id}`, `DELETE /admin/categories/{id}` (soft delete → riwayat)
- `PATCH /admin/categories/{id}/toggle` — `{is_voting_open?, show_live_results?}`
- `GET /admin/categories/trash` — daftar kategori di riwayat
- `POST /admin/categories/{id}/restore` — pulihkan dari riwayat
- `DELETE /admin/categories/{id}/permanent` — hapus permanen (harus sudah di riwayat
  & belum pernah ada suara, kalau tidak ditolak 422/409)
- `POST /admin/categories`, `PUT /admin/categories/{id}` juga menerima
  `voting_start_at`/`voting_end_at` (format `YYYY-MM-DD HH:MM:SS`, opsional) — kalau
  diisi, status buka/tutup kategori otomatis disesuaikan jadwal ini setiap kali
  kategori dibaca (mengambil alih toggle manual selama jadwal aktif)
- `GET /admin/categories/{id}/non-voters` — daftar siswa yang belum memilih di kategori ini
- `GET /admin/categories/{id}/export` — download hasil voting sebagai CSV
- `GET /admin/candidates?category_id=`
- `POST /admin/candidates` — multipart: `category_id, name, description, photo`
- `PUT|POST /admin/candidates/{id}` — multipart, foto opsional
- `DELETE /admin/candidates/{id}` — soft delete
- `GET /admin/students`
- `POST /admin/students` — `{name, nis}`
- `PUT /admin/students/{id}` — `{name, nis}`
- `DELETE /admin/students/{id}` — soft delete (tidak bisa login lagi, suara lama tetap tersimpan)
- `POST /admin/students/import` — multipart CSV (`file`), kolom: `name,nis`
- `POST /admin/students/{id}/reset-password`
- `GET /admin/categories/{id}/results` — hasil lengkap + siswa tercepat memilih + counter
  (tetap bisa diakses untuk kategori yang sudah di riwayat)
- `POST /admin/admins` — `{name, username}` — buat akun admin baru, password awal `username*`
- `GET /admin/activity-logs` — log aktivitas admin (200 terbaru), berisi siapa/aksi/kapan

## 6. Keamanan yang sudah diterapkan

- Semua query pakai PDO + prepared statement (tidak ada string concatenation ke SQL).
- Password di-hash dengan `password_hash()` / diverifikasi dengan `password_verify()`.
- `UNIQUE(user_id, category_id)` di tabel `votes` mencegah satu siswa memilih dua kali
  di kategori yang sama, bahkan kalau ada race condition (double-tap) — dijaga di dua
  level: pre-check sebelum insert, dan constraint DB sebagai jaring pengaman race condition.
- `POST /votes` menolak (403) request dari user yang `must_change_password` masih `1`,
  jadi "wajib ganti password dulu" tidak hanya jadi gate di sisi Android yang bisa
  dilewati lewat panggilan API langsung (mis. Postman).
- Kategori & siswa yang dihapus admin selalu **soft delete** (`deleted_at`), bukan
  hilang dari database. Siswa yang dihapus langsung tidak bisa login lagi (dicek
  saat login MAUPUN di setiap request via token lama, jadi sesi yang sedang aktif
  juga langsung mati), tapi suara yang sudah dia masukkan tetap tersimpan & terhitung.
- Kategori hanya bisa dihapus **permanen** kalau (a) sudah di-soft-delete dulu, DAN
  (b) belum pernah ada satu suara pun masuk -- mencegah data suara dihapus diam-diam
  oleh siapa pun, termasuk admin sendiri.
- Upload foto kandidat: validasi MIME dari isi file asli (bukan dari ekstensi/Content-Type
  kiriman klien), maksimal 2MB, nama file di-randomize sebelum disimpan.
- `.htaccess` memblokir akses langsung ke `*.sql`, `*.md`, dan folder `config/`,
  `core/`, `handlers/` — tanpa ini file-file itu bisa dibuka langsung lewat browser
  walaupun tidak ditautkan ke router manapun.
- `uploads/.htaccess` mematikan eksekusi PHP/script di folder upload, sebagai
  lapisan pertahanan kedua kalau validasi upload suatu saat punya celah.
- Kandidat dihapus admin selalu soft delete (`is_active=0`); hasil voting tetap
  menghitung suara historis kandidat yang sudah dihapus supaya total & persentase
  tidak berubah diam-diam, tapi kandidat itu tidak lagi muncul di layar pemilihan.
- Error internal (exception, query gagal) ditangkap di satu titik (`index.php`)
  dan dikembalikan sebagai pesan generik — tidak ada stack trace/path server yang
  bocor ke response API.
- Setiap aksi mutasi admin (kategori, kandidat, siswa, akun admin) tercatat di
  `activity_logs` (siapa, aksi apa, kapan) -- tidak ada endpoint untuk mengubah/menghapus
  log ini dari aplikasi, supaya tetap jadi jejak yang bisa dipercaya kalau perlu ditelusuri.
  Relevan terutama karena sekarang bisa ada lebih dari satu akun admin.

### Risiko yang **sengaja** dibiarkan (bawaan dari desain produk, bukan bug)

- Password default siswa (`NIS` + `*`) bisa ditebak siapa pun yang tahu NIS targetnya
  — ini bawaan dari spec produk, bukan kesalahan kode. Mitigasi: bagikan kredensial
  privat per siswa, minta ganti password di sesi terawasi sebelum voting dibuka untuk umum.
- Belum ada rate limiting / lockout di `/auth/login`. Untuk skala sekolah di LAN lokal
  ini ditoleransi, tapi kasih tahu kalau kamu mau ditambahkan.
