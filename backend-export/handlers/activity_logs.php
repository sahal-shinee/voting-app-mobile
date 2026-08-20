<?php

// Audit trail aksi admin. Tidak ada endpoint update/delete untuk tabel ini --
// dibuat read-only dari sisi aplikasi supaya jadi jejak yang bisa dipercaya.
function logActivity(PDO $pdo, int $adminId, string $action, string $targetType, ?int $targetId, string $description): void
{
    $stmt = $pdo->prepare(
        'INSERT INTO activity_logs (admin_id, action, target_type, target_id, description, created_at)
         VALUES (?, ?, ?, ?, ?, NOW())'
    );
    $stmt->execute([$adminId, $action, $targetType, $targetId, $description]);
}

function adminActivityLogsIndex(PDO $pdo): void
{
    Auth::requireAdmin($pdo);

    $stmt = $pdo->query(
        'SELECT l.*, u.name AS admin_name FROM activity_logs l
         JOIN users u ON u.id = l.admin_id
         ORDER BY l.created_at DESC, l.id DESC
         LIMIT 200'
    );

    $result = array_map(function (array $row): array {
        return [
            'id' => (int) $row['id'],
            'admin_name' => $row['admin_name'],
            'action' => $row['action'],
            'target_type' => $row['target_type'],
            'target_id' => $row['target_id'] !== null ? (int) $row['target_id'] : null,
            'description' => $row['description'],
            'created_at' => $row['created_at'],
        ];
    }, $stmt->fetchAll());

    Response::success($result);
}
