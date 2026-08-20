<?php

function votesCreate(PDO $pdo): void
{
    $user = Auth::requireAuth($pdo);

    // Server-side gate: "wajib ganti password dulu" tidak boleh hanya jadi
    // pemeriksaan di sisi Android, karena bisa dilewati lewat panggilan API
    // langsung (Postman dll).
    if ((bool) $user['must_change_password']) {
        Response::error('Ganti password default kamu dulu sebelum memilih', 403);
    }

    $body = json_decode(file_get_contents('php://input'), true) ?? [];
    $categoryId = $body['category_id'] ?? null;
    $candidateId = $body['candidate_id'] ?? null;

    if (!$categoryId || !$candidateId) {
        Response::error('category_id dan candidate_id wajib diisi', 422);
    }

    applyCategorySchedule($pdo);
    $category = findCategoryOrFail($pdo, $categoryId);

    if (!$category['is_voting_open']) {
        Response::error('Voting untuk kategori ini sudah ditutup', 409);
    }

    $stmt = $pdo->prepare('SELECT id FROM candidates WHERE id = ? AND category_id = ? AND is_active = 1');
    $stmt->execute([$candidateId, $categoryId]);
    if (!$stmt->fetch()) {
        Response::error('Kandidat tidak valid untuk kategori ini', 422);
    }

    $stmt = $pdo->prepare('SELECT id FROM votes WHERE user_id = ? AND category_id = ?');
    $stmt->execute([$user['id'], $categoryId]);
    if ($stmt->fetch()) {
        Response::error('Kamu sudah memilih di kategori ini', 409);
    }

    try {
        $stmt = $pdo->prepare(
            'INSERT INTO votes (user_id, category_id, candidate_id, created_at) VALUES (?, ?, ?, NOW())'
        );
        $stmt->execute([$user['id'], $categoryId, $candidateId]);
    } catch (PDOException $e) {
        // Jaring pengaman terhadap race condition (double-tap / 2 request bersamaan):
        // pre-check di atas bisa lolos berbarengan, tapi UNIQUE(user_id, category_id)
        // di level DB tetap akan menolak salah satu insert.
        if ($e->getCode() === '23000') {
            Response::error('Kamu sudah memilih di kategori ini', 409);
        }
        throw $e;
    }

    Response::success(['vote_id' => (int) $pdo->lastInsertId()], 'Suara berhasil dicatat', 201);
}
