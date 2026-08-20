<?php

function formatStudent(array $u): array
{
    return [
        'id' => (int) $u['id'],
        'name' => $u['name'],
        'nis' => $u['nis'],
        'must_change_password' => (bool) $u['must_change_password'],
        'created_at' => $u['created_at'],
    ];
}

function adminStudentsIndex(PDO $pdo): void
{
    Auth::requireAdmin($pdo);
    $stmt = $pdo->query("SELECT * FROM users WHERE role = 'student' AND deleted_at IS NULL ORDER BY name ASC");
    Response::success(array_map('formatStudent', $stmt->fetchAll()));
}

function adminStudentsCreate(PDO $pdo): void
{
    $admin = Auth::requireAdmin($pdo);
    $body = json_decode(file_get_contents('php://input'), true) ?? [];
    $name = trim($body['name'] ?? '');
    $nis = trim($body['nis'] ?? '');

    if ($name === '' || $nis === '') {
        Response::error('Nama dan NIS wajib diisi', 422);
    }

    $stmt = $pdo->prepare('SELECT id FROM users WHERE nis = ?');
    $stmt->execute([$nis]);
    if ($stmt->fetch()) {
        Response::error('NIS sudah terdaftar', 409);
    }

    $password = $nis . '*';
    $hash = password_hash($password, PASSWORD_DEFAULT);

    $stmt = $pdo->prepare(
        "INSERT INTO users (name, nis, username, password, role, must_change_password, created_at)
         VALUES (?, ?, ?, ?, 'student', 1, NOW())"
    );
    $stmt->execute([$name, $nis, $nis, $hash]);

    $newId = (int) $pdo->lastInsertId();
    logActivity($pdo, (int) $admin['id'], 'create', 'student', $newId, "Menambahkan siswa \"{$name}\" (NIS {$nis})");

    Response::success([
        'id' => $newId,
        'name' => $name,
        'nis' => $nis,
        'initial_password' => $password,
    ], 'Siswa berhasil ditambahkan', 201);
}

function adminStudentsUpdate(PDO $pdo, $id): void
{
    $admin = Auth::requireAdmin($pdo);

    $stmt = $pdo->prepare("SELECT * FROM users WHERE id = ? AND role = 'student' AND deleted_at IS NULL");
    $stmt->execute([$id]);
    $student = $stmt->fetch();
    if (!$student) {
        Response::error('Siswa tidak ditemukan', 404);
    }

    $body = json_decode(file_get_contents('php://input'), true) ?? [];
    $name = trim($body['name'] ?? $student['name']);
    $nis = trim($body['nis'] ?? $student['nis']);

    if ($name === '' || $nis === '') {
        Response::error('Nama dan NIS wajib diisi', 422);
    }

    if ($nis !== $student['nis']) {
        $stmt = $pdo->prepare('SELECT id FROM users WHERE nis = ? AND id != ?');
        $stmt->execute([$nis, $id]);
        if ($stmt->fetch()) {
            Response::error('NIS sudah dipakai siswa lain', 409);
        }
    }

    $stmt = $pdo->prepare('UPDATE users SET name = ?, nis = ?, username = ? WHERE id = ?');
    $stmt->execute([$name, $nis, $nis, $id]);

    logActivity($pdo, (int) $admin['id'], 'update', 'student', (int) $id, "Mengubah data siswa \"{$name}\"");

    $stmt = $pdo->prepare('SELECT * FROM users WHERE id = ?');
    $stmt->execute([$id]);
    Response::success(formatStudent($stmt->fetch()), 'Siswa berhasil diperbarui');
}

// Soft delete: akun di-nonaktifkan (tidak bisa login lagi, token lama langsung mati),
// tapi baris user TIDAK dihapus -- supaya riwayat suara yang sudah dia masukkan tetap
// tersimpan & terhitung dengan benar di hasil voting.
function adminStudentsDelete(PDO $pdo, $id): void
{
    $admin = Auth::requireAdmin($pdo);

    $stmt = $pdo->prepare("SELECT * FROM users WHERE id = ? AND role = 'student' AND deleted_at IS NULL");
    $stmt->execute([$id]);
    $student = $stmt->fetch();
    if (!$student) {
        Response::error('Siswa tidak ditemukan', 404);
    }

    $stmt = $pdo->prepare('UPDATE users SET deleted_at = NOW() WHERE id = ?');
    $stmt->execute([$id]);

    $stmt = $pdo->prepare('DELETE FROM tokens WHERE user_id = ?');
    $stmt->execute([$id]);

    logActivity($pdo, (int) $admin['id'], 'delete', 'student', (int) $id, "Menghapus siswa \"{$student['name']}\"");

    Response::success(null, 'Siswa berhasil dihapus');
}

function adminStudentsImport(PDO $pdo): void
{
    $admin = Auth::requireAdmin($pdo);

    if (empty($_FILES['file']) || $_FILES['file']['error'] !== UPLOAD_ERR_OK) {
        Response::error('File CSV wajib diunggah', 422);
    }

    // Deteksi tipe file dari isi sebenarnya, bukan dari nama/Content-Type kiriman
    // klien -- ini menolak file .xlsx/.xls yang "diganti nama" jadi .csv (file Excel
    // asli berupa arsip ZIP biner, kalau dipaksa diparse fgetcsv() bisa membuat PHP
    // macet/timeout mengunyah data biner sebagai baris CSV, bukan error yang rapi).
    $finfo = new finfo(FILEINFO_MIME_TYPE);
    $mime = $finfo->file($_FILES['file']['tmp_name']);
    if (strpos($mime, 'text/') !== 0) {
        Response::error(
            'File yang diunggah bukan CSV asli (sepertinya masih format Excel .xlsx/.xls). '
            . 'Buka file itu, lalu gunakan "Save As" / "Simpan Sebagai" dan pilih format CSV (.csv) sebelum upload.',
            422
        );
    }

    $handle = fopen($_FILES['file']['tmp_name'], 'r');
    if (!$handle) {
        Response::error('File tidak bisa dibaca', 422);
    }

    // Lewati BOM UTF-8 kalau ada -- file CSV yang disimpan lewat Excel/Sheets di
    // HP sering diawali 3 byte ini, dan tanpa dilewati baris header gagal dikenali.
    if (fread($handle, 3) !== "\xEF\xBB\xBF") {
        rewind($handle);
    }

    $checkStmt = $pdo->prepare('SELECT id FROM users WHERE nis = ?');
    $insertStmt = $pdo->prepare(
        "INSERT INTO users (name, nis, username, password, role, must_change_password, created_at)
         VALUES (?, ?, ?, ?, 'student', 1, NOW())"
    );

    $created = [];
    $skipped = [];
    $rowNum = 0;

    while (($row = fgetcsv($handle)) !== false) {
        $rowNum++;
        if ($rowNum === 1 && strtolower(trim($row[0] ?? '')) === 'name') {
            continue;
        }

        $name = trim($row[0] ?? '');
        $nis = trim($row[1] ?? '');

        if ($name === '' || $nis === '') {
            $skipped[] = ['row' => $rowNum, 'reason' => 'Data tidak lengkap'];
            continue;
        }

        if (mb_strlen($name) > 150) {
            $skipped[] = ['row' => $rowNum, 'reason' => 'Nama terlalu panjang (maks 150 karakter)'];
            continue;
        }

        if (mb_strlen($nis) > 50) {
            $skipped[] = ['row' => $rowNum, 'nis' => $nis, 'reason' => 'NIS terlalu panjang (maks 50 karakter)'];
            continue;
        }

        $checkStmt->execute([$nis]);
        if ($checkStmt->fetch()) {
            $skipped[] = ['row' => $rowNum, 'nis' => $nis, 'reason' => 'NIS sudah terdaftar'];
            continue;
        }

        // Satu baris bermasalah (mis. tabrakan unik di level DB yang lolos
        // pre-check di atas) tidak boleh menggagalkan seluruh import -- baris
        // lain yang valid harus tetap tersimpan.
        try {
            $hash = password_hash($nis . '*', PASSWORD_DEFAULT);
            $insertStmt->execute([$name, $nis, $nis, $hash]);
            $created[] = ['name' => $name, 'nis' => $nis];
        } catch (PDOException $e) {
            $skipped[] = ['row' => $rowNum, 'nis' => $nis, 'reason' => 'Gagal menyimpan baris ini'];
        }
    }

    fclose($handle);

    logActivity(
        $pdo,
        (int) $admin['id'],
        'import',
        'student',
        null,
        'Import siswa: ' . count($created) . ' berhasil, ' . count($skipped) . ' dilewati'
    );

    Response::success([
        'created' => $created,
        'created_count' => count($created),
        'skipped' => $skipped,
        'skipped_count' => count($skipped),
    ], 'Import selesai');
}

function adminStudentsResetPassword(PDO $pdo, $id): void
{
    $admin = Auth::requireAdmin($pdo);

    $stmt = $pdo->prepare("SELECT * FROM users WHERE id = ? AND role = 'student' AND deleted_at IS NULL");
    $stmt->execute([$id]);
    $student = $stmt->fetch();

    if (!$student) {
        Response::error('Siswa tidak ditemukan', 404);
    }

    $password = $student['nis'] . '*';
    $hash = password_hash($password, PASSWORD_DEFAULT);

    $stmt = $pdo->prepare('UPDATE users SET password = ?, must_change_password = 1 WHERE id = ?');
    $stmt->execute([$hash, $id]);

    logActivity($pdo, (int) $admin['id'], 'reset_password', 'student', (int) $id, "Reset password siswa \"{$student['name']}\"");

    Response::success([
        'id' => (int) $id,
        'initial_password' => $password,
    ], 'Password berhasil di-reset');
}
