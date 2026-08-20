<?php
// Jalankan SATU KALI kalau lupa password admin, untuk reset password admin
// kembali ke default (admin123) dan must_change_password=1 lagi.
//
//   CLI    : php reset_admin.php
//   Browser: http://localhost/<folder-project>/reset_admin.php
//
// File ini menghapus dirinya sendiri setelah selesai dijalankan, dan (sama
// seperti seed.php) lewat browser hanya bisa diakses dari localhost -- supaya
// tidak bisa dipanggil siapa pun di LAN untuk membajak akun admin.

if (PHP_SAPI !== 'cli') {
    $remoteAddr = $_SERVER['REMOTE_ADDR'] ?? '';
    if (!in_array($remoteAddr, ['127.0.0.1', '::1'], true)) {
        http_response_code(403);
        header('Content-Type: text/plain');
        echo "Akses ditolak. reset_admin.php hanya bisa dijalankan dari localhost.\n";
        echo "Buka browser di komputer server itu sendiri, atau jalankan lewat CLI: php reset_admin.php\n";
        exit;
    }
}

require __DIR__ . '/config/database.php';

$newPassword = 'admin123';
$hash = password_hash($newPassword, PASSWORD_DEFAULT);

$stmt = $pdo->prepare("UPDATE users SET password = ?, must_change_password = 1 WHERE nis = 'admin' AND role = 'admin'");
$stmt->execute([$hash]);

header('Content-Type: text/plain');

if ($stmt->rowCount() === 0) {
    echo "Tidak ada akun admin dengan nis='admin' yang ditemukan. Tidak ada yang diubah.\n";
} else {
    echo "Selesai. Password admin berhasil di-reset.\n";
    echo "Admin -> username: admin / password: {$newPassword}\n";
    echo "Kamu akan diminta ganti password lagi saat login (opsional, bisa di-skip).\n";
}

@unlink(__FILE__);
echo "\nreset_admin.php sudah menghapus dirinya sendiri.\n";
