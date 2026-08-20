<?php

// Jadwal voting otomatis (opsional). Dipanggil di setiap titik masuk yang
// membaca/menegakkan status buka-tutup kategori, supaya is_voting_open selalu
// konsisten dengan voting_start_at/voting_end_at tanpa perlu cron job terpisah.
// Kategori tanpa jadwal (kedua kolom NULL) sama sekali tidak tersentuh -- tetap
// 100% dikontrol manual seperti sebelumnya. Selama jadwal terisi, jadwal yang
// menentukan, mengambil alih toggle manual.
function applyCategorySchedule(PDO $pdo): void
{
    $now = date('Y-m-d H:i:s');

    $pdo->prepare(
        'UPDATE categories SET is_voting_open = 0
         WHERE deleted_at IS NULL AND voting_start_at IS NOT NULL AND voting_start_at > ? AND is_voting_open = 1'
    )->execute([$now]);

    $pdo->prepare(
        'UPDATE categories SET is_voting_open = 1
         WHERE deleted_at IS NULL AND voting_start_at IS NOT NULL AND voting_start_at <= ?
           AND (voting_end_at IS NULL OR voting_end_at > ?) AND is_voting_open = 0'
    )->execute([$now, $now]);

    $pdo->prepare(
        'UPDATE categories SET is_voting_open = 0
         WHERE deleted_at IS NULL AND voting_end_at IS NOT NULL AND voting_end_at <= ? AND is_voting_open = 1'
    )->execute([$now]);
}

function categoriesIndex(PDO $pdo): void
{
    $user = Auth::requireAuth($pdo);
    applyCategorySchedule($pdo);

    $stmt = $pdo->query('SELECT * FROM categories WHERE deleted_at IS NULL ORDER BY created_at DESC, id DESC');
    $categories = $stmt->fetchAll();

    $voteStmt = $pdo->prepare('SELECT id FROM votes WHERE user_id = ? AND category_id = ?');

    $result = [];
    foreach ($categories as $cat) {
        $voteStmt->execute([$user['id'], $cat['id']]);
        $result[] = [
            'id' => (int) $cat['id'],
            'name' => $cat['name'],
            'description' => $cat['description'],
            'is_voting_open' => (bool) $cat['is_voting_open'],
            'show_live_results' => (bool) $cat['show_live_results'],
            'has_voted' => (bool) $voteStmt->fetch(),
        ];
    }

    Response::success($result);
}

function formatCategory(array $c): array
{
    return [
        'id' => (int) $c['id'],
        'name' => $c['name'],
        'description' => $c['description'],
        'is_voting_open' => (bool) $c['is_voting_open'],
        'show_live_results' => (bool) $c['show_live_results'],
        'voting_start_at' => $c['voting_start_at'] ?? null,
        'voting_end_at' => $c['voting_end_at'] ?? null,
        'created_at' => $c['created_at'],
        'deleted_at' => $c['deleted_at'] ?? null,
    ];
}

// Terima string "YYYY-MM-DD HH:MM:SS" dari Android, atau null/"" untuk hapus
// jadwal. Validasi longgar (format saja) karena MySQL akan menolak nilai yang
// benar-benar tidak valid sebagai DATETIME.
function parseScheduleInput($value): ?string
{
    if ($value === null) {
        return null;
    }
    $value = trim((string) $value);
    if ($value === '') {
        return null;
    }
    if (!preg_match('/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/', $value)) {
        Response::error('Format jadwal tidak valid', 422);
    }
    return $value;
}

// Dipakai oleh operasi normal (voting, kandidat, toggle, dst) -- kategori yang
// sudah di-soft-delete dianggap "tidak ada" di sini, sesuai cakupannya.
function findCategoryOrFail(PDO $pdo, $id): array
{
    $stmt = $pdo->prepare('SELECT * FROM categories WHERE id = ? AND deleted_at IS NULL');
    $stmt->execute([$id]);
    $category = $stmt->fetch();

    if (!$category) {
        Response::error('Kategori tidak ditemukan', 404);
    }

    return $category;
}

// Dipakai khusus oleh layar admin yang memang perlu melihat kategori apa pun,
// termasuk yang sudah di-soft-delete (mis. hasil di halaman riwayat).
function findCategoryAnyState(PDO $pdo, $id): array
{
    $stmt = $pdo->prepare('SELECT * FROM categories WHERE id = ?');
    $stmt->execute([$id]);
    $category = $stmt->fetch();

    if (!$category) {
        Response::error('Kategori tidak ditemukan', 404);
    }

    return $category;
}

function adminCategoriesIndex(PDO $pdo): void
{
    Auth::requireAdmin($pdo);
    applyCategorySchedule($pdo);
    $stmt = $pdo->query('SELECT * FROM categories WHERE deleted_at IS NULL ORDER BY created_at DESC, id DESC');
    Response::success(array_map('formatCategory', $stmt->fetchAll()));
}

function adminCategoriesCreate(PDO $pdo): void
{
    $admin = Auth::requireAdmin($pdo);
    $body = json_decode(file_get_contents('php://input'), true) ?? [];
    $name = trim($body['name'] ?? '');
    $description = $body['description'] ?? null;
    $votingStartAt = parseScheduleInput($body['voting_start_at'] ?? null);
    $votingEndAt = parseScheduleInput($body['voting_end_at'] ?? null);

    if ($name === '') {
        Response::error('Nama kategori wajib diisi', 422);
    }

    $stmt = $pdo->prepare(
        'INSERT INTO categories (name, description, is_voting_open, show_live_results, voting_start_at, voting_end_at, created_at)
         VALUES (?, ?, 0, 1, ?, ?, NOW())'
    );
    $stmt->execute([$name, $description, $votingStartAt, $votingEndAt]);

    $newId = (int) $pdo->lastInsertId();
    logActivity($pdo, (int) $admin['id'], 'create', 'category', $newId, "Membuat kategori \"{$name}\"");

    $category = findCategoryOrFail($pdo, $newId);
    Response::success(formatCategory($category), 'Kategori berhasil dibuat', 201);
}

function adminCategoriesUpdate(PDO $pdo, $id): void
{
    $admin = Auth::requireAdmin($pdo);
    $category = findCategoryOrFail($pdo, $id);

    $body = json_decode(file_get_contents('php://input'), true) ?? [];
    $name = trim($body['name'] ?? $category['name']);
    $description = $body['description'] ?? $category['description'];
    $votingStartAt = array_key_exists('voting_start_at', $body)
        ? parseScheduleInput($body['voting_start_at'])
        : $category['voting_start_at'];
    $votingEndAt = array_key_exists('voting_end_at', $body)
        ? parseScheduleInput($body['voting_end_at'])
        : $category['voting_end_at'];

    if ($name === '') {
        Response::error('Nama kategori wajib diisi', 422);
    }

    $stmt = $pdo->prepare(
        'UPDATE categories SET name = ?, description = ?, voting_start_at = ?, voting_end_at = ? WHERE id = ?'
    );
    $stmt->execute([$name, $description, $votingStartAt, $votingEndAt, $id]);

    logActivity($pdo, (int) $admin['id'], 'update', 'category', (int) $id, "Mengubah kategori \"{$name}\"");

    Response::success(formatCategory(findCategoryOrFail($pdo, $id)), 'Kategori berhasil diperbarui');
}

// Soft delete: kategori dipindahkan ke "riwayat", tidak hilang dari database.
// Selalu boleh, terlepas dari ada/tidaknya kandidat -- beda dari hapus permanen.
function adminCategoriesDelete(PDO $pdo, $id): void
{
    $admin = Auth::requireAdmin($pdo);
    $category = findCategoryOrFail($pdo, $id);

    $stmt = $pdo->prepare('UPDATE categories SET deleted_at = NOW() WHERE id = ?');
    $stmt->execute([$id]);

    logActivity($pdo, (int) $admin['id'], 'delete', 'category', (int) $id, "Memindahkan kategori \"{$category['name']}\" ke riwayat");

    Response::success(null, 'Kategori dipindahkan ke riwayat');
}

function adminCategoriesTrash(PDO $pdo): void
{
    Auth::requireAdmin($pdo);
    $stmt = $pdo->query('SELECT * FROM categories WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC');
    Response::success(array_map('formatCategory', $stmt->fetchAll()));
}

function adminCategoriesRestore(PDO $pdo, $id): void
{
    $admin = Auth::requireAdmin($pdo);

    $stmt = $pdo->prepare('SELECT * FROM categories WHERE id = ? AND deleted_at IS NOT NULL');
    $stmt->execute([$id]);
    $category = $stmt->fetch();
    if (!$category) {
        Response::error('Kategori tidak ditemukan di riwayat', 404);
    }

    $stmt = $pdo->prepare('UPDATE categories SET deleted_at = NULL WHERE id = ?');
    $stmt->execute([$id]);

    logActivity($pdo, (int) $admin['id'], 'restore', 'category', (int) $id, "Memulihkan kategori \"{$category['name']}\" dari riwayat");

    Response::success(null, 'Kategori berhasil dipulihkan');
}

// Hapus permanen HANYA untuk kategori yang sudah di riwayat (soft-deleted dulu)
// DAN belum pernah ada suara sama sekali -- supaya integritas data suara tidak
// pernah bisa dihapus diam-diam, walau oleh admin sendiri.
function adminCategoriesPermanentDelete(PDO $pdo, $id): void
{
    $admin = Auth::requireAdmin($pdo);

    $stmt = $pdo->prepare('SELECT * FROM categories WHERE id = ?');
    $stmt->execute([$id]);
    $category = $stmt->fetch();
    if (!$category) {
        Response::error('Kategori tidak ditemukan', 404);
    }

    if ($category['deleted_at'] === null) {
        Response::error('Kategori harus dihapus (masuk riwayat) dulu sebelum bisa dihapus permanen', 422);
    }

    $stmt = $pdo->prepare('SELECT COUNT(*) FROM votes WHERE category_id = ?');
    $stmt->execute([$id]);
    if ((int) $stmt->fetchColumn() > 0) {
        Response::error(
            'Kategori tidak bisa dihapus permanen karena sudah ada suara yang masuk. Kategori tetap berada di riwayat dan bisa dipulihkan.',
            409
        );
    }

    $stmt = $pdo->prepare('DELETE FROM candidates WHERE category_id = ?');
    $stmt->execute([$id]);

    $stmt = $pdo->prepare('DELETE FROM categories WHERE id = ?');
    $stmt->execute([$id]);

    logActivity($pdo, (int) $admin['id'], 'permanent_delete', 'category', (int) $id, "Menghapus permanen kategori \"{$category['name']}\"");

    Response::success(null, 'Kategori berhasil dihapus permanen');
}

function adminCategoriesToggle(PDO $pdo, $id): void
{
    $admin = Auth::requireAdmin($pdo);
    $category = findCategoryOrFail($pdo, $id);

    $body = json_decode(file_get_contents('php://input'), true) ?? [];
    $isVotingOpen = array_key_exists('is_voting_open', $body)
        ? (int) (bool) $body['is_voting_open']
        : (int) $category['is_voting_open'];
    $showLiveResults = array_key_exists('show_live_results', $body)
        ? (int) (bool) $body['show_live_results']
        : (int) $category['show_live_results'];

    $stmt = $pdo->prepare('UPDATE categories SET is_voting_open = ?, show_live_results = ? WHERE id = ?');
    $stmt->execute([$isVotingOpen, $showLiveResults, $id]);

    $statusText = $isVotingOpen ? 'membuka' : 'menutup';
    logActivity($pdo, (int) $admin['id'], 'toggle', 'category', (int) $id, "Mengubah status kategori \"{$category['name']}\" ({$statusText} voting)");

    Response::success(formatCategory(findCategoryOrFail($pdo, $id)), 'Status kategori berhasil diperbarui');
}
