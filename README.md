# LyricsEmbedder — Ứng dụng Android gắn Metadata & Nhúng Lời Bài Hát

Ứng dụng Android viết bằng **Kotlin + Jetpack Compose + Material 3 (Material You)**, hỗ trợ **điện thoại & máy tính bảng**, tương thích từ **Android 5.0 (API 21)** đến **Android 15 (API 35)**.

## Tính năng chính

1. **Thanh tiêu đề (Header)**
   - Tên ứng dụng `LyricsEmbedder` bên trái, 2 biểu tượng âm nhạc ở giữa, menu 3 chấm bên phải gồm **Giới thiệu** và **Cài đặt**.
2. **Tab 1 — Gắn Metadata (`Gắn Metadata`)**
   - Quét thư mục và lọc riêng các tệp âm thanh hỗ trợ gắn tag (`MP3`, `FLAC`, `M4A`, `MP4`, `OGG`, `OPUS`, `WAV`, `WMA`, `AIFF`, `DSF`).
   - Tự động nhận diện và sắp xếp thứ tự các tệp chưa có metadata đặt tên dạng `Track 1`, `Track 2`, `01 Track 1.flac`... theo đúng thứ tự album.
   - Hỗ trợ nút **Set metadata for this folder** (và **Set metadata** cho từng tệp nhạc) mở bảng tìm kiếm & ánh xạ theo phong cách MusicBee:
     - Tìm kiếm trên **MusicBrainz API** theo `album artist`, `album`, `track on album`, `disc #`, `tracks on disc`.
     - Hiển thị bảng kết quả (`type`, `country`, `year`, `album`, `album artist`, `publisher`, `source`).
     - Xem trước ảnh bìa từ **Cover Art Archive** (dự phòng iTunes Search API) và chỉnh sửa từng trường (`album artist`, `composer`, `album`, `publisher`, `disc number`, `year`, `genre`).
     - Bảng ánh xạ bài hát (`checkbox`, `#`, `artist`, `title`, `time`, `original title`, `time`, `filename`).
     - Nhấn **Apply** để nhúng metadata + ảnh bìa trực tiếp vào từng tệp âm thanh và tự động lưu thêm 1 tệp ảnh bìa (`Cover.jpg`) vào thư mục đích.
   - Hỗ trợ sửa nhanh metadata album thủ công và áp dụng đồng loạt cho toàn bộ bài hát trong thư mục.
3. **Tab 2 — Thêm Lyrics (`Thêm Lyrics`)**
   - Quét toàn bộ bài hát trong các thư mục đã chọn với 3 bộ lọc: **Không có lời**, **Đã có lời**, **Chưa có metadata** (tệp chưa có metadata bị khóa không cho tải lời).
   - Chọn tất cả bài chưa có lời hoặc chọn từng bài để tải và nhúng/lưu lời bài hát từ **LrcLib API**.
   - **BottomSheet kéo lên/xuống**: hiển thị tiến trình tải trực tiếp theo từng bài và cho phép chọn nhanh bài hát cần tải ngay trong bảng dưới.
   - **Trình chỉnh sửa lời bài hát**: chỉnh sửa trực tiếp lời LRC/thường, nút dịch dấu thời gian `- / +` theo bước cấu hình, và tùy chọn xử lý bằng AI (**Google Gemini** / **OpenRouter** dùng khóa API cá nhân lưu cục bộ).
4. **Cài đặt đầy đủ 6 danh mục**
   - **Chủ đề**: Sáng / Tối / Theo hệ thống + Màu động **Material You** (Android 12+).
   - **Yêu cầu lời bài hát**: Tìm kiếm mở rộng, Thông báo khi thất bại, Thay thế thông báo trước đó, Loại lời ưu tiên (Đã đồng bộ / Text-only), Đánh dấu bài không lời.
   - **Cài đặt lưu trữ lời bài hát**: Gửi lời đến Poweramp, Lưu dạng tệp `.lrc`/`.txt`, Nhúng lời vào tệp, Sửa siêu dữ liệu bài hát, Tùy chọn tag ID3v2.3/ID3v1 & ảnh bìa `Cover.jpg`, Quản lý **Thư mục nhạc từ Poweramp** và **Thư mục tùy chỉnh đã chọn** (SAF).
   - **Nhà cung cấp lời bài hát**: Quản lý danh sách URL API tương thích LrcLib (mặc định `https://lrclib.net/api`) và chọn API đang hoạt động.
   - **Trình chỉnh sửa lời bài hát**: Cấu hình khóa API & mô hình cho Google Gemini (`gemini-2.5-flash`) và OpenRouter (`openrouter/free`), Bước điều chỉnh dấu thời gian (`10 phần trăm giây` = 100ms...).
   - **Bộ lọc**: Bộ lọc tiêu đề, Bộ lọc nghệ sĩ, Bộ lọc album (không phân biệt chữ hoa/thường, hỗ trợ Regex).

## Cách mở và build dự án

1. Giải nén tệp `LyricsEmbedder_Android_Project.zip`.
2. Mở thư mục `LyricsEmbedder` bằng **Android Studio Ladybug (2024.2+)** trở lên với **JDK 17**.
3. Chờ Android Studio đồng bộ Gradle (`Gradle Sync`), sau đó nhấn **Run** để cài lên điện thoại/máy tính bảng Android 5.0+ hoặc build APK bằng `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
