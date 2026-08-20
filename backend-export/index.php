<?php
declare(strict_types=1);

error_reporting(E_ALL);
ini_set('display_errors', '0');

// ---- CORS ----
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

require __DIR__ . '/config/database.php';
require __DIR__ . '/core/Response.php';
require __DIR__ . '/core/Auth.php';
require __DIR__ . '/core/Router.php';
require __DIR__ . '/core/Upload.php';

require __DIR__ . '/handlers/auth.php';
require __DIR__ . '/handlers/categories.php';
require __DIR__ . '/handlers/candidates.php';
require __DIR__ . '/handlers/votes.php';
require __DIR__ . '/handlers/results.php';
require __DIR__ . '/handlers/students.php';
require __DIR__ . '/handlers/admins.php';
require __DIR__ . '/handlers/activity_logs.php';

// PHP tidak mengisi $_POST/$_FILES untuk request PUT bermedia multipart/form-data.
// Parse manual supaya endpoint admin yang menerima PUT+multipart tetap berfungsi.
if ($_SERVER['REQUEST_METHOD'] === 'PUT' && str_starts_with($_SERVER['CONTENT_TYPE'] ?? '', 'multipart/form-data')) {
    require __DIR__ . '/core/MultipartParser.php';
    $parsed = MultipartParser::parse();
    $_POST = $parsed['post'];
    $_FILES = $parsed['files'];
}

$router = new Router();

// ---- Auth ----
$router->post('/auth/login', fn () => authLogin($pdo));
$router->post('/auth/change-password', fn () => authChangePassword($pdo));
$router->post('/auth/logout', fn () => authLogout($pdo));

// ---- Siswa ----
$router->get('/categories', fn () => categoriesIndex($pdo));
$router->get('/categories/{id}/candidates', fn ($id) => candidatesByCategory($pdo, $id));
$router->post('/votes', fn () => votesCreate($pdo));
$router->get('/categories/{id}/results', fn ($id) => resultsForCategory($pdo, $id));
$router->get('/me/votes', fn () => meVotes($pdo));

// ---- Admin: categories ----
$router->get('/admin/categories', fn () => adminCategoriesIndex($pdo));
$router->post('/admin/categories', fn () => adminCategoriesCreate($pdo));
$router->put('/admin/categories/{id}', fn ($id) => adminCategoriesUpdate($pdo, $id));
$router->delete('/admin/categories/{id}', fn ($id) => adminCategoriesDelete($pdo, $id));
$router->patch('/admin/categories/{id}/toggle', fn ($id) => adminCategoriesToggle($pdo, $id));
$router->get('/admin/categories/{id}/results', fn ($id) => adminResultsForCategory($pdo, $id));
$router->get('/admin/categories/{id}/non-voters', fn ($id) => adminCategoryNonVoters($pdo, $id));
$router->get('/admin/categories/{id}/export', fn ($id) => adminCategoryExportCsv($pdo, $id));
$router->get('/admin/categories/trash', fn () => adminCategoriesTrash($pdo));
$router->post('/admin/categories/{id}/restore', fn ($id) => adminCategoriesRestore($pdo, $id));
$router->delete('/admin/categories/{id}/permanent', fn ($id) => adminCategoriesPermanentDelete($pdo, $id));

// ---- Admin: candidates ----
$router->get('/admin/candidates', fn () => adminCandidatesIndex($pdo));
$router->post('/admin/candidates', fn () => adminCandidatesCreate($pdo));
$router->put('/admin/candidates/{id}', fn ($id) => adminCandidatesUpdate($pdo, $id));
$router->post('/admin/candidates/{id}', fn ($id) => adminCandidatesUpdate($pdo, $id));
$router->delete('/admin/candidates/{id}', fn ($id) => adminCandidatesDelete($pdo, $id));

// ---- Admin: students ----
$router->get('/admin/students', fn () => adminStudentsIndex($pdo));
$router->post('/admin/students', fn () => adminStudentsCreate($pdo));
$router->put('/admin/students/{id}', fn ($id) => adminStudentsUpdate($pdo, $id));
$router->delete('/admin/students/{id}', fn ($id) => adminStudentsDelete($pdo, $id));
$router->post('/admin/students/import', fn () => adminStudentsImport($pdo));
$router->post('/admin/students/{id}/reset-password', fn ($id) => adminStudentsResetPassword($pdo, $id));

// ---- Admin: akun admin ----
$router->post('/admin/admins', fn () => adminCreateAdmin($pdo));

// ---- Admin: log aktivitas ----
$router->get('/admin/activity-logs', fn () => adminActivityLogsIndex($pdo));

try {
    $uri = $_GET['url'] ?? '/';
    $router->dispatch($_SERVER['REQUEST_METHOD'], $uri);
} catch (Throwable $e) {
    error_log('[SuaraKita API] ' . $e->getMessage());
    Response::error('Terjadi kesalahan pada server', 500);
}
