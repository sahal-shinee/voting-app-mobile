<?php

class Response
{
    public static function json(bool $success, $data = null, string $message = '', int $statusCode = 200): void
    {
        http_response_code($statusCode);
        header('Content-Type: application/json');
        echo json_encode([
            'success' => $success,
            'data' => $data,
            'message' => $message,
        ]);
        exit;
    }

    public static function success($data = null, string $message = 'OK', int $statusCode = 200): void
    {
        self::json(true, $data, $message, $statusCode);
    }

    public static function error(string $message = 'Terjadi kesalahan', int $statusCode = 400, $data = null): void
    {
        self::json(false, $data, $message, $statusCode);
    }
}
