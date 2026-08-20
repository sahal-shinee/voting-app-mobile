<?php

function authLogin(PDO $pdo): void
{
    $body = json_decode(file_get_contents('php://input'), true) ?? [];
    $identifier = trim($body['identifier'] ?? '');
    $password = $body['password'] ?? '';

    if ($identifier === '' || $password === '') {
        Response::error('Identifier dan password wajib diisi', 422);
    }

    $stmt = $pdo->prepare('SELECT * FROM users WHERE (username = ? OR nis = ?) AND deleted_at IS NULL LIMIT 1');
    $stmt->execute([$identifier, $identifier]);
    $user = $stmt->fetch();

    if (!$user || !password_verify($password, $user['password'])) {
        Response::error('NIS/username atau password salah', 401);
    }

    $token = Auth::generateToken();
    $stmt = $pdo->prepare('INSERT INTO tokens (user_id, token, created_at) VALUES (?, ?, NOW())');
    $stmt->execute([$user['id'], $token]);

    Response::success([
        'token' => $token,
        'user' => [
            'id' => (int) $user['id'],
            'name' => $user['name'],
            'role' => $user['role'],
            'must_change_password' => (bool) $user['must_change_password'],
        ],
    ], 'Login berhasil');
}

function authChangePassword(PDO $pdo): void
{
    $user = Auth::requireAuth($pdo);
    $body = json_decode(file_get_contents('php://input'), true) ?? [];
    $oldPassword = $body['old_password'] ?? '';
    $newPassword = $body['new_password'] ?? '';

    if ($oldPassword === '' || $newPassword === '') {
        Response::error('Password lama dan password baru wajib diisi', 422);
    }

    if (strlen($newPassword) < 6) {
        Response::error('Password baru minimal 6 karakter', 422);
    }

    $stmt = $pdo->prepare('SELECT password FROM users WHERE id = ?');
    $stmt->execute([$user['id']]);
    $row = $stmt->fetch();

    if (!password_verify($oldPassword, $row['password'])) {
        Response::error('Password lama tidak sesuai', 401);
    }

    if (password_verify($newPassword, $row['password'])) {
        Response::error('Password baru tidak boleh sama dengan password lama', 422);
    }

    $hash = password_hash($newPassword, PASSWORD_DEFAULT);
    $stmt = $pdo->prepare('UPDATE users SET password = ?, must_change_password = 0 WHERE id = ?');
    $stmt->execute([$hash, $user['id']]);

    Response::success(null, 'Password berhasil diubah');
}

function authLogout(PDO $pdo): void
{
    Auth::requireAuth($pdo);
    $token = Auth::currentToken();

    $stmt = $pdo->prepare('DELETE FROM tokens WHERE token = ?');
    $stmt->execute([$token]);

    Response::success(null, 'Logout berhasil');
}
