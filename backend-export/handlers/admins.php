<?php

// Buat akun admin baru. Tidak ada "NIS" untuk admin -- nis & username diisi nilai
// yang sama (username yang dipilih), mengikuti pola akun admin seed bawaan.
function adminCreateAdmin(PDO $pdo): void
{
    $admin = Auth::requireAdmin($pdo);

    $body = json_decode(file_get_contents('php://input'), true) ?? [];
    $name = trim($body['name'] ?? '');
    $username = trim($body['username'] ?? '');

    if ($name === '' || $username === '') {
        Response::error('Nama dan username wajib diisi', 422);
    }

    $stmt = $pdo->prepare('SELECT id FROM users WHERE nis = ? OR username = ?');
    $stmt->execute([$username, $username]);
    if ($stmt->fetch()) {
        Response::error('Username sudah dipakai', 409);
    }

    $password = $username . '*';
    $hash = password_hash($password, PASSWORD_DEFAULT);

    $stmt = $pdo->prepare(
        "INSERT INTO users (name, nis, username, password, role, must_change_password, created_at)
         VALUES (?, ?, ?, ?, 'admin', 1, NOW())"
    );
    $stmt->execute([$name, $username, $username, $hash]);

    $newId = (int) $pdo->lastInsertId();
    logActivity($pdo, (int) $admin['id'], 'create', 'admin', $newId, "Menambahkan akun admin \"{$name}\" ({$username})");

    Response::success([
        'id' => $newId,
        'name' => $name,
        'username' => $username,
        'initial_password' => $password,
    ], 'Akun admin berhasil dibuat', 201);
}
