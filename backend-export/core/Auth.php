<?php

class Auth
{
    private static function bearerToken(): ?string
    {
        $headers = function_exists('getallheaders') ? getallheaders() : [];
        $authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? ($_SERVER['HTTP_AUTHORIZATION'] ?? '');

        if (!preg_match('/Bearer\s+(.+)/i', $authHeader, $m)) {
            return null;
        }

        return trim($m[1]);
    }

    public static function user(PDO $pdo): ?array
    {
        $token = self::bearerToken();
        if (!$token) {
            return null;
        }

        $stmt = $pdo->prepare(
            'SELECT u.* FROM tokens t JOIN users u ON u.id = t.user_id
             WHERE t.token = ? AND u.deleted_at IS NULL LIMIT 1'
        );
        $stmt->execute([$token]);
        $user = $stmt->fetch();

        return $user ?: null;
    }

    public static function requireAuth(PDO $pdo): array
    {
        $user = self::user($pdo);
        if (!$user) {
            Response::error('Unauthorized', 401);
        }

        return $user;
    }

    public static function requireAdmin(PDO $pdo): array
    {
        $user = self::requireAuth($pdo);
        if ($user['role'] !== 'admin') {
            Response::error('Forbidden', 403);
        }

        return $user;
    }

    public static function currentToken(): ?string
    {
        return self::bearerToken();
    }

    public static function generateToken(): string
    {
        return bin2hex(random_bytes(32));
    }
}
