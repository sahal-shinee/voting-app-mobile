-- Suara Kita - database schema + seed data
-- Import file ini lewat phpMyAdmin atau:
--   mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS suarakita CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE suarakita;

-- users: siswa + admin
CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(150) NOT NULL,
  nis VARCHAR(50) NOT NULL UNIQUE,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role ENUM('student','admin') NOT NULL DEFAULT 'student',
  must_change_password TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL
) ENGINE=InnoDB;

-- categories: OSIS, Ekskul, Guru Favorit, dst (CRUD penuh oleh admin)
-- deleted_at = soft-delete (masuk "riwayat"); NULL berarti masih aktif.
-- voting_start_at/voting_end_at = jadwal otomatis (opsional). Kalau NULL,
-- voting dibuka/ditutup manual lewat is_voting_open seperti biasa.
CREATE TABLE categories (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(150) NOT NULL,
  description TEXT NULL,
  is_voting_open TINYINT(1) NOT NULL DEFAULT 0,
  show_live_results TINYINT(1) NOT NULL DEFAULT 1,
  voting_start_at DATETIME NULL,
  voting_end_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL
) ENGINE=InnoDB;

-- candidates: milik 1 kategori
CREATE TABLE candidates (
  id INT AUTO_INCREMENT PRIMARY KEY,
  category_id INT NOT NULL,
  name VARCHAR(150) NOT NULL,
  photo VARCHAR(255) NULL,
  description TEXT NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_candidates_category FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB;

-- votes: satu baris = satu suara. UNIQUE(user_id, category_id) = inti integritas "1 suara/kategori"
CREATE TABLE votes (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  category_id INT NOT NULL,
  candidate_id INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_user_category UNIQUE (user_id, category_id),
  CONSTRAINT fk_votes_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_votes_category FOREIGN KEY (category_id) REFERENCES categories(id),
  CONSTRAINT fk_votes_candidate FOREIGN KEY (candidate_id) REFERENCES candidates(id)
) ENGINE=InnoDB;

-- tokens: auth sederhana (bearer token)
CREATE TABLE tokens (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  token VARCHAR(255) NOT NULL UNIQUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- activity_logs: audit trail aksi admin (siapa, apa, kapan). Read-only dari
-- sisi aplikasi -- tidak ada endpoint update/delete, supaya jadi jejak yang bisa
-- dipercaya kalau ada yang perlu ditelusuri (penting sekarang karena admin bisa lebih dari satu).
CREATE TABLE activity_logs (
  id INT AUTO_INCREMENT PRIMARY KEY,
  admin_id INT NOT NULL,
  action VARCHAR(100) NOT NULL,
  target_type VARCHAR(50) NOT NULL,
  target_id INT NULL,
  description VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_activity_logs_admin FOREIGN KEY (admin_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- ============================================================
-- SEED DATA
-- ============================================================
-- PENTING: kolom password di bawah ini BUKAN hash yang valid (login akan
-- selalu gagal sampai kamu menjalankannya). Setelah import schema ini,
-- jalankan seed.php SATU KALI (lewat browser atau CLI `php seed.php`) untuk
-- mengisi hash bcrypt asli sesuai password default di README. seed.php akan
-- menghapus dirinya sendiri setelah selesai dijalankan.

INSERT INTO users (name, nis, username, password, role, must_change_password) VALUES
('Administrator', 'admin', 'admin', 'RUN_SEED_PHP_TO_SET_PASSWORD', 'admin', 1),
('Andi Saputra', '2024001', '2024001', 'RUN_SEED_PHP_TO_SET_PASSWORD', 'student', 1),
('Bunga Lestari', '2024002', '2024002', 'RUN_SEED_PHP_TO_SET_PASSWORD', 'student', 1),
('Citra Dewi', '2024003', '2024003', 'RUN_SEED_PHP_TO_SET_PASSWORD', 'student', 1);

INSERT INTO categories (name, description, is_voting_open, show_live_results) VALUES
('Ketua OSIS', 'Pemilihan Ketua OSIS periode berjalan', 1, 1),
('Ekskul Favorit', 'Pemilihan ekstrakurikuler favorit siswa', 1, 1),
('Guru Favorit', 'Pemilihan guru favorit pilihan siswa', 0, 1);

INSERT INTO candidates (category_id, name, description, is_active) VALUES
(1, 'Kandidat A', 'Calon Ketua OSIS nomor urut 1', 1),
(1, 'Kandidat B', 'Calon Ketua OSIS nomor urut 2', 1),
(1, 'Kandidat C', 'Calon Ketua OSIS nomor urut 3', 1),
(2, 'Basket', 'Ekstrakurikuler Basket', 1),
(2, 'Futsal', 'Ekstrakurikuler Futsal', 1),
(2, 'Pramuka', 'Ekstrakurikuler Pramuka', 1),
(3, 'Bapak Guru A', 'Guru Matematika', 1),
(3, 'Ibu Guru B', 'Guru Bahasa Indonesia', 1),
(3, 'Bapak Guru C', 'Guru Olahraga', 1);
