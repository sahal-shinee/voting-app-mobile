<?php

// PHP hanya mem-parsing body multipart/form-data ke $_POST/$_FILES untuk request
// POST. Endpoint admin yang menerima PUT+multipart (update kandidat tanpa ganti
// foto/dengan ganti foto) butuh parser manual ini supaya tetap berfungsi.
class MultipartParser
{
    public static function parse(): array
    {
        $contentType = $_SERVER['CONTENT_TYPE'] ?? '';
        if (!preg_match('/boundary=(.+)$/', $contentType, $m)) {
            return ['post' => [], 'files' => []];
        }

        $boundary = trim($m[1], '"');
        $rawData = file_get_contents('php://input');
        $blocks = preg_split('/-{2}' . preg_quote($boundary, '/') . '/', $rawData);

        $post = [];
        $files = [];

        foreach ($blocks as $block) {
            $block = ltrim($block, "\r\n");
            if ($block === '' || $block === '--' || $block === "--\r\n") {
                continue;
            }

            if (strpos($block, 'filename="') !== false) {
                preg_match('/name="([^"]*)"/', $block, $nameMatch);
                preg_match('/filename="([^"]*)"/', $block, $fileMatch);
                preg_match('/Content-Type:\s*([^\r\n]+)/', $block, $typeMatch);

                $fieldName = $nameMatch[1] ?? '';
                $fileName = $fileMatch[1] ?? '';
                $mimeType = trim($typeMatch[1] ?? 'application/octet-stream');

                if ($fieldName === '' || $fileName === '') {
                    continue;
                }

                $parts = preg_split('/\r\n\r\n/', $block, 2);
                $content = isset($parts[1]) ? preg_replace('/\r\n$/', '', $parts[1]) : '';

                $tmpPath = tempnam(sys_get_temp_dir(), 'upl');
                file_put_contents($tmpPath, $content);

                $files[$fieldName] = [
                    'name' => $fileName,
                    'type' => $mimeType,
                    'tmp_name' => $tmpPath,
                    'error' => UPLOAD_ERR_OK,
                    'size' => strlen($content),
                ];
            } else {
                preg_match('/name="([^"]*)"/', $block, $nameMatch);
                if (!isset($nameMatch[1])) {
                    continue;
                }

                $parts = preg_split('/\r\n\r\n/', $block, 2);
                $value = isset($parts[1]) ? trim($parts[1], "\r\n") : '';
                $post[$nameMatch[1]] = $value;
            }
        }

        return ['post' => $post, 'files' => $files];
    }
}
