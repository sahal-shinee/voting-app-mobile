<?php
// Jalankan SATU KALI setelah import schema.sql, untuk mengisi password hash asli
// (schema.sql sengaja diisi placeholder yang tidak valid, bukan hash beneran).
//
//   CLI    : php seed.php
//   Browser: http://localhost/<folder-project>/seed.php
//
// Password default:
//   admin   -> admin123
//   siswa   -> <NIS>*
//
// File ini menghapus dirinya sendiri setelah selesai dijalankan supaya tidak
// bisa dipanggil ulang untuk mereset password ke nilai default secara diam-diam.
//
// Script ini tidak butuh login, jadi selama masih ada di server, siapa pun yang
// tahu URL-nya bisa memanggilnya. Sebagai lapisan pertahanan, akses lewat browser
// hanya diizinkan dari localhost -- dari komputer server itu sendiri.

if (PHP_SAPI !== 'cli') {
    $remoteAddr = $_SERVER['REMOTE_ADDR'] ?? '';
    if (!in_array($remoteAddr, ['127.0.0.1', '::1'], true)) {
        http_response_code(403);
        header('Content-Type: text/plain');
        echo "Akses ditolak. seed.php hanya bisa dijalankan dari localhost.\n";
        echo "Buka browser di komputer server itu sendiri, atau jalankan lewat CLI: php seed.php\n";
        exit;
    }
}

require __DIR__ . '/config/database.php';

$accounts = [
    ['nis' => 'admin', 'password' => 'admin123'],
];

$stmt = $pdo->query("SELECT nis FROM users WHERE role = 'student'");
foreach ($stmt->fetchAll(PDO::FETCH_COLUMN) as $nis) {
    $accounts[] = ['nis' => $nis, 'password' => $nis . '*'];
}

$update = $pdo->prepare('UPDATE users SET password = ? WHERE nis = ?');
$count = 0;
foreach ($accounts as $acc) {
    $hash = password_hash($acc['password'], PASSWORD_DEFAULT);
    $update->execute([$hash, $acc['nis']]);
    $count++;
}

header('Content-Type: text/plain');
echo "Selesai. {$count} password berhasil di-set.\n";
echo "Admin -> username: admin / password: admin123\n";
echo "Siswa -> username: <NIS> / password: <NIS>*\n";

@unlink(__FILE__);
echo "\nseed.php sudah menghapus dirinya sendiri.\n";
