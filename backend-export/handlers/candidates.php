<?php

function formatCandidatePublic(array $c): array
{
    return [
        'id' => (int) $c['id'],
        'category_id' => (int) $c['category_id'],
        'name' => $c['name'],
        'photo_url' => candidatePhotoUrl($c['photo']),
        'description' => $c['description'],
    ];
}

function formatCandidateAdmin(array $c): array
{
    $base = formatCandidatePublic($c);
    $base['is_active'] = (bool) $c['is_active'];
    $base['created_at'] = $c['created_at'];

    return $base;
}

function candidatePhotoUrl(?string $photo): ?string
{
    if (!$photo) {
        return null;
    }

    return baseUrl() . '/' . $photo;
}

function baseUrl(): string
{
    $scheme = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
    $host = $_SERVER['HTTP_HOST'] ?? 'localhost';
    $scriptDir = rtrim(str_replace('\\', '/', dirname($_SERVER['SCRIPT_NAME'])), '/');

    return $scheme . '://' . $host . $scriptDir;
}

function findCandidateOrFail(PDO $pdo, $id): array
{
    $stmt = $pdo->prepare('SELECT * FROM candidates WHERE id = ?');
    $stmt->execute([$id]);
    $candidate = $stmt->fetch();

    if (!$candidate) {
        Response::error('Kandidat tidak ditemukan', 404);
    }

    return $candidate;
}

function candidatesByCategory(PDO $pdo, $categoryId): void
{
    Auth::requireAuth($pdo);
    findCategoryOrFail($pdo, $categoryId);

    $stmt = $pdo->prepare('SELECT * FROM candidates WHERE category_id = ? AND is_active = 1 ORDER BY id ASC');
    $stmt->execute([$categoryId]);

    Response::success(array_map('formatCandidatePublic', $stmt->fetchAll()));
}

function adminCandidatesIndex(PDO $pdo): void
{
    Auth::requireAdmin($pdo);
    $categoryId = $_GET['category_id'] ?? null;

    if ($categoryId) {
        $stmt = $pdo->prepare('SELECT * FROM candidates WHERE category_id = ? ORDER BY id ASC');
        $stmt->execute([$categoryId]);
    } else {
        $stmt = $pdo->query('SELECT * FROM candidates ORDER BY id ASC');
    }

    Response::success(array_map('formatCandidateAdmin', $stmt->fetchAll()));
}

function adminCandidatesCreate(PDO $pdo): void
{
    $admin = Auth::requireAdmin($pdo);

    $categoryId = $_POST['category_id'] ?? null;
    $name = trim($_POST['name'] ?? '');
    $description = $_POST['description'] ?? null;

    if (!$categoryId || $name === '') {
        Response::error('category_id dan name wajib diisi', 422);
    }

    findCategoryOrFail($pdo, $categoryId);

    $photoPath = null;
    if (!empty($_FILES['photo']) && $_FILES['photo']['error'] !== UPLOAD_ERR_NO_FILE) {
        $photoPath = Upload::saveCandidatePhoto($_FILES['photo']);
    }

    $stmt = $pdo->prepare(
        'INSERT INTO candidates (category_id, name, photo, description, is_active, created_at) VALUES (?, ?, ?, ?, 1, NOW())'
    );
    $stmt->execute([$categoryId, $name, $photoPath, $description]);

    $newId = (int) $pdo->lastInsertId();
    logActivity($pdo, (int) $admin['id'], 'create', 'candidate', $newId, "Menambahkan kandidat \"{$name}\"");

    $candidate = findCandidateOrFail($pdo, $newId);
    Response::success(formatCandidateAdmin($candidate), 'Kandidat berhasil ditambahkan', 201);
}

function adminCandidatesUpdate(PDO $pdo, $id): void
{
    $admin = Auth::requireAdmin($pdo);
    $candidate = findCandidateOrFail($pdo, $id);

    $name = trim($_POST['name'] ?? $candidate['name']);
    $description = $_POST['description'] ?? $candidate['description'];
    $categoryId = $_POST['category_id'] ?? $candidate['category_id'];

    if ($name === '') {
        Response::error('Nama kandidat wajib diisi', 422);
    }

    if ((string) $categoryId !== (string) $candidate['category_id']) {
        findCategoryOrFail($pdo, $categoryId);
    }

    $photoPath = $candidate['photo'];
    if (!empty($_FILES['photo']) && $_FILES['photo']['error'] !== UPLOAD_ERR_NO_FILE) {
        $newPhoto = Upload::saveCandidatePhoto($_FILES['photo']);
        Upload::deleteCandidatePhoto($candidate['photo']);
        $photoPath = $newPhoto;
    }

    $stmt = $pdo->prepare('UPDATE candidates SET category_id = ?, name = ?, description = ?, photo = ? WHERE id = ?');
    $stmt->execute([$categoryId, $name, $description, $photoPath, $id]);

    logActivity($pdo, (int) $admin['id'], 'update', 'candidate', (int) $id, "Mengubah kandidat \"{$name}\"");

    Response::success(formatCandidateAdmin(findCandidateOrFail($pdo, $id)), 'Kandidat berhasil diperbarui');
}

function adminCandidatesDelete(PDO $pdo, $id): void
{
    $admin = Auth::requireAdmin($pdo);
    $candidate = findCandidateOrFail($pdo, $id);

    $stmt = $pdo->prepare('UPDATE candidates SET is_active = 0 WHERE id = ?');
    $stmt->execute([$id]);

    logActivity($pdo, (int) $admin['id'], 'delete', 'candidate', (int) $id, "Menghapus kandidat \"{$candidate['name']}\"");

    Response::success(null, 'Kandidat berhasil dihapus');
}
