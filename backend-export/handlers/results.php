<?php

function buildResultsPayload(PDO $pdo, array $category): array
{
    // Kandidat yang masih aktif selalu ditampilkan (termasuk yang belum ada suara),
    // dan kandidat yang sudah di-soft-delete tetap ditampilkan KALAU sudah pernah
    // menerima suara -- supaya total_votes & persentase tetap akurat secara historis
    // (alasan soft-delete di spec memang supaya suara yang sudah masuk tidak hilang).
    $stmt = $pdo->prepare(
        'SELECT c.id, c.name, c.photo, c.is_active, COUNT(v.id) AS vote_count
         FROM candidates c
         LEFT JOIN votes v ON v.candidate_id = c.id
         WHERE c.category_id = ?
         GROUP BY c.id
         HAVING c.is_active = 1 OR vote_count > 0
         ORDER BY vote_count DESC, c.id ASC'
    );
    $stmt->execute([$category['id']]);
    $rows = $stmt->fetchAll();

    $total = array_sum(array_map(fn ($r) => (int) $r['vote_count'], $rows));

    $candidates = array_map(function (array $r) use ($total): array {
        $count = (int) $r['vote_count'];

        return [
            'candidate_id' => (int) $r['id'],
            'name' => $r['name'],
            'photo_url' => candidatePhotoUrl($r['photo']),
            'is_active' => (bool) $r['is_active'],
            'vote_count' => $count,
            'percentage' => $total > 0 ? round($count * 100 / $total, 1) : 0.0,
        ];
    }, $rows);

    return [
        'category_id' => (int) $category['id'],
        'category_name' => $category['name'],
        'is_voting_open' => (bool) $category['is_voting_open'],
        'show_live_results' => (bool) $category['show_live_results'],
        'total_votes' => $total,
        'candidates' => $candidates,
    ];
}

function resultsForCategory(PDO $pdo, $categoryId): void
{
    $user = Auth::requireAuth($pdo);
    $category = findCategoryOrFail($pdo, $categoryId);

    $isAdmin = $user['role'] === 'admin';
    if (!$isAdmin && !$category['show_live_results'] && $category['is_voting_open']) {
        Response::error('Hasil belum bisa ditampilkan', 403);
    }

    Response::success(buildResultsPayload($pdo, $category));
}

function adminResultsForCategory(PDO $pdo, $categoryId): void
{
    Auth::requireAdmin($pdo);
    // Pakai findCategoryAnyState (bukan findCategoryOrFail) -- admin tetap boleh
    // lihat hasil kategori yang sudah di-soft-delete (riwayat), supaya bisa jadi
    // bahan pertimbangan sebelum pulihkan/hapus permanen.
    $category = findCategoryAnyState($pdo, $categoryId);

    $payload = buildResultsPayload($pdo, $category);

    $stmt = $pdo->prepare(
        'SELECT u.name, u.nis, v.created_at
         FROM votes v
         JOIN users u ON u.id = v.user_id
         WHERE v.category_id = ?
         ORDER BY v.created_at ASC
         LIMIT 10'
    );
    $stmt->execute([$categoryId]);

    $payload['fastest_voters'] = array_map(fn (array $r): array => [
        'name' => $r['name'],
        'nis' => $r['nis'],
        'voted_at' => $r['created_at'],
    ], $stmt->fetchAll());

    $stmt = $pdo->prepare('SELECT COUNT(*) FROM votes WHERE category_id = ?');
    $stmt->execute([$categoryId]);
    $payload['voted_count'] = (int) $stmt->fetchColumn();

    $payload['total_students'] = (int) $pdo->query(
        "SELECT COUNT(*) FROM users WHERE role = 'student' AND deleted_at IS NULL"
    )->fetchColumn();

    Response::success($payload);
}

// Export hasil voting jadi CSV (bisa langsung dibuka di Excel/Sheets). Pure
// PHP fputcsv, tanpa library tambahan -- sengaja dipilih ketimbang PDF supaya
// tidak menambah dependency yang belum bisa diuji jalan di server PHP siapa pun.
function adminCategoryExportCsv(PDO $pdo, $categoryId): void
{
    Auth::requireAdmin($pdo);
    $category = findCategoryAnyState($pdo, $categoryId);
    $payload = buildResultsPayload($pdo, $category);

    $filename = 'hasil-' . preg_replace('/[^a-zA-Z0-9_-]+/', '-', $category['name']) . '.csv';

    header('Content-Type: text/csv; charset=utf-8');
    header('Content-Disposition: attachment; filename="' . $filename . '"');

    $out = fopen('php://output', 'w');
    fputcsv($out, ['Kandidat', 'Jumlah Suara', 'Persentase']);
    foreach ($payload['candidates'] as $candidate) {
        fputcsv($out, [$candidate['name'], $candidate['vote_count'], $candidate['percentage'] . '%']);
    }
    fputcsv($out, []);
    fputcsv($out, ['Total Suara', $payload['total_votes']]);
    fclose($out);
}

// Daftar siswa aktif yang belum memilih di kategori ini -- supaya panitia bisa
// follow up langsung sebelum voting ditutup.
function adminCategoryNonVoters(PDO $pdo, $categoryId): void
{
    Auth::requireAdmin($pdo);
    findCategoryAnyState($pdo, $categoryId);

    $stmt = $pdo->prepare(
        "SELECT u.id, u.name, u.nis FROM users u
         WHERE u.role = 'student' AND u.deleted_at IS NULL
           AND u.id NOT IN (SELECT user_id FROM votes WHERE category_id = ?)
         ORDER BY u.name ASC"
    );
    $stmt->execute([$categoryId]);

    Response::success(array_map(fn (array $r): array => [
        'id' => (int) $r['id'],
        'name' => $r['name'],
        'nis' => $r['nis'],
    ], $stmt->fetchAll()));
}

function meVotes(PDO $pdo): void
{
    $user = Auth::requireAuth($pdo);

    $stmt = $pdo->prepare(
        'SELECT v.category_id, v.candidate_id, c.name AS candidate_name, v.created_at
         FROM votes v
         JOIN candidates c ON c.id = v.candidate_id
         WHERE v.user_id = ?'
    );
    $stmt->execute([$user['id']]);

    Response::success(array_map(fn (array $r): array => [
        'category_id' => (int) $r['category_id'],
        'candidate_id' => (int) $r['candidate_id'],
        'candidate_name' => $r['candidate_name'],
        'voted_at' => $r['created_at'],
    ], $stmt->fetchAll()));
}
