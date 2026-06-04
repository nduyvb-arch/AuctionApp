package org.example.server.manager;

import org.example.common.model.item.Item;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AntiSniper}.
 *
 * Coverage targets:
 *  - static  : applyAntiSniper()
 *  - instance: constructor (1-arg & 3-arg), isExpired(), getRemainingMillis(),
 *              checkAndExtend(), getEndTime(), getRemainingFormatted()
 *
 * Dependencies (add to pom.xml / build.gradle if not already present):
 *   - org.junit.jupiter:junit-jupiter:5.10.x
 *   - org.mockito:mockito-core:5.x
 *   - org.mockito:mockito-junit-jupiter:5.x
 */
class AntiSniperTest {

    // -------------------------------------------------------------------------
    // Static method: applyAntiSniper(Item)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("applyAntiSniper()")
    class ApplyAntiSniperTests {

        @Test
        @DisplayName("Trả về false khi endTime là null")
        void shouldReturnFalse_whenEndTimeIsNull() {
            Item item = mock(Item.class);
            when(item.getEndTime()).thenReturn(null);

            boolean result = AntiSniper.applyAntiSniper(item);

            assertFalse(result);
            verify(item, never()).setEndTime(any());
        }

        @Test
        @DisplayName("Trả về false khi endTime đã qua (trong quá khứ)")
        void shouldReturnFalse_whenEndTimeIsInPast() {
            Item item = mock(Item.class);
            // milisLeft sẽ <= 0 → không trigger
            when(item.getEndTime()).thenReturn(LocalDateTime.now().minusSeconds(10));

            boolean result = AntiSniper.applyAntiSniper(item);

            assertFalse(result);
            verify(item, never()).setEndTime(any());
        }

        @Test
        @DisplayName("Trả về false khi endTime còn hơn 30 giây (ngoài ngưỡng)")
        void shouldReturnFalse_whenEndTimeIsMoreThan30SecondsAway() {
            Item item = mock(Item.class);
            // 60 giây còn lại > TRIGGER_THRESHOLD_MILLIS (30 000 ms) → không trigger
            when(item.getEndTime()).thenReturn(LocalDateTime.now().plusSeconds(60));

            boolean result = AntiSniper.applyAntiSniper(item);

            assertFalse(result);
            verify(item, never()).setEndTime(any());
        }

        @Test
        @DisplayName("Trả về true và gia hạn thêm 30 giây khi còn trong ngưỡng")
        void shouldReturnTrue_andExtendEndTime_whenWithinThreshold() {
            Item item = mock(Item.class);
            LocalDateTime nearFuture = LocalDateTime.now().plusSeconds(15); // 15 s < 30 s threshold
            when(item.getEndTime()).thenReturn(nearFuture);
            when(item.getItemName()).thenReturn("Laptop Gaming");

            boolean result = AntiSniper.applyAntiSniper(item);

            assertTrue(result);
            // setEndTime phải được gọi với nearFuture + 30s
            verify(item).setEndTime(nearFuture.plusSeconds(30));
        }

        @Test
        @DisplayName("Trả về true ngay tại biên ngưỡng (1 mili-giây trước khi hết hạn)")
        void shouldReturnTrue_whenEndTimeIsAtThresholdBoundary() {
            Item item = mock(Item.class);
            // Đúng 1 mili-giây trước khi hết ngưỡng 30 000 ms → vẫn trigger
            LocalDateTime justInsideThreshold = LocalDateTime.now().plusSeconds(29).plusNanos(999_000_000L);
            when(item.getEndTime()).thenReturn(justInsideThreshold);
            when(item.getItemName()).thenReturn("Điện thoại");

            boolean result = AntiSniper.applyAntiSniper(item);

            assertTrue(result);
            verify(item).setEndTime(justInsideThreshold.plusSeconds(30));
        }
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor 1-arg: sử dụng ngưỡng và gia hạn mặc định (30 s)")
        void oneArgConstructor_shouldUseDefaultThresholdAndExtension() {
            AntiSniper antiSniper = new AntiSniper(60_000L); // 60 s

            assertFalse(antiSniper.isExpired(), "Phiên đấu giá không được hết hạn ngay lập tức");
            assertTrue(antiSniper.getRemainingMillis() > 0);
        }

        @Test
        @DisplayName("Constructor 3-arg: sử dụng ngưỡng và gia hạn tùy chỉnh")
        void threeArgConstructor_shouldUseCustomThresholdAndExtension() {
            AntiSniper antiSniper = new AntiSniper(60_000L, 10_000L, 15_000L);

            assertFalse(antiSniper.isExpired());
            assertTrue(antiSniper.getRemainingMillis() > 0);
        }

        @Test
        @DisplayName("Constructor với duration âm → phiên hết hạn ngay lập tức")
        void constructor_withNegativeDuration_shouldBeExpiredImmediately() {
            AntiSniper antiSniper = new AntiSniper(-1L);

            assertTrue(antiSniper.isExpired());
            assertEquals(0L, antiSniper.getRemainingMillis());
        }
    }

    // -------------------------------------------------------------------------
    // isExpired()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("isExpired()")
    class IsExpiredTests {

        @Test
        @DisplayName("Trả về false khi phiên còn thời gian")
        void shouldReturnFalse_whenSessionIsStillActive() {
            AntiSniper antiSniper = new AntiSniper(60_000L);

            assertFalse(antiSniper.isExpired());
        }

        @Test
        @DisplayName("Trả về true khi phiên đã hết hạn")
        void shouldReturnTrue_whenSessionHasExpired() {
            AntiSniper antiSniper = new AntiSniper(-1000L); // duration âm → đã hết hạn

            assertTrue(antiSniper.isExpired());
        }
    }

    // -------------------------------------------------------------------------
    // getRemainingMillis()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getRemainingMillis()")
    class GetRemainingMillisTests {

        @Test
        @DisplayName("Trả về giá trị dương khi phiên còn thời gian")
        void shouldReturnPositiveValue_whenSessionIsActive() {
            AntiSniper antiSniper = new AntiSniper(60_000L);

            long remaining = antiSniper.getRemainingMillis();

            assertTrue(remaining > 0);
            assertTrue(remaining <= 60_000L);
        }

        @Test
        @DisplayName("Trả về 0 (không âm) khi phiên đã hết hạn")
        void shouldReturnZero_whenSessionIsExpired() {
            AntiSniper antiSniper = new AntiSniper(-1000L);

            long remaining = antiSniper.getRemainingMillis();

            assertEquals(0L, remaining, "getRemainingMillis() không được trả về giá trị âm");
        }
    }

    // -------------------------------------------------------------------------
    // checkAndExtend()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("checkAndExtend()")
    class CheckAndExtendTests {

        @Test
        @DisplayName("Gia hạn và trả về true khi thời gian còn lại nhỏ hơn ngưỡng")
        void shouldExtendAndReturnTrue_whenRemainingIsBelowThreshold() {
            // 5 s còn lại < ngưỡng mặc định 30 s → trigger
            AntiSniper antiSniper = new AntiSniper(5_000L);
            long endTimeBefore = antiSniper.getEndTime();

            boolean result = antiSniper.checkAndExtend();

            assertTrue(result, "checkAndExtend() phải trả về true khi phát hiện snipe");
            assertTrue(antiSniper.getEndTime() > endTimeBefore,
                    "endTime phải được gia hạn sau khi phát hiện snipe");
        }

        @Test
        @DisplayName("Không gia hạn và trả về false khi thời gian còn lại lớn hơn ngưỡng")
        void shouldNotExtendAndReturnFalse_whenRemainingIsAboveThreshold() {
            // 60 s còn lại, ngưỡng 10 s → không trigger
            AntiSniper antiSniper = new AntiSniper(60_000L, 10_000L, 15_000L);
            long endTimeBefore = antiSniper.getEndTime();

            boolean result = antiSniper.checkAndExtend();

            assertFalse(result, "checkAndExtend() phải trả về false khi chưa chạm ngưỡng");
            assertEquals(endTimeBefore, antiSniper.getEndTime(),
                    "endTime không được thay đổi");
        }

        @Test
        @DisplayName("Gia hạn đúng số mili-giây được cấu hình")
        void shouldExtendByConfiguredMillis() {
            long extensionMillis = 15_000L;
            AntiSniper antiSniper = new AntiSniper(5_000L, 30_000L, extensionMillis);
            long endTimeBefore = antiSniper.getEndTime();

            antiSniper.checkAndExtend();

            assertEquals(endTimeBefore + extensionMillis, antiSniper.getEndTime(),
                    "endTime phải tăng đúng bằng extensionMillis");
        }
    }

    // -------------------------------------------------------------------------
    // getEndTime()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getEndTime()")
    class GetEndTimeTests {

        @Test
        @DisplayName("Trả về endTime xấp xỉ currentTimeMillis + duration")
        void shouldReturnApproximatelyCurrentTimePlusDuration() {
            long duration = 60_000L;
            long before = System.currentTimeMillis();
            AntiSniper antiSniper = new AntiSniper(duration);
            long after = System.currentTimeMillis();

            long endTime = antiSniper.getEndTime();

            assertTrue(endTime >= before + duration,
                    "endTime phải >= thời điểm tạo + duration");
            assertTrue(endTime <= after + duration + 50,
                    "endTime không được vượt quá thời điểm tạo + duration + 50ms");
        }
    }

    // -------------------------------------------------------------------------
    // getRemainingFormatted()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getRemainingFormatted()")
    class GetRemainingFormattedTests {

        @Test
        @DisplayName("Định dạng đúng MM:SS khi còn thời gian")
        void shouldReturnCorrectMMSSFormat() {
            AntiSniper antiSniper = new AntiSniper(90_000L); // 1 phút 30 giây

            String formatted = antiSniper.getRemainingFormatted();

            assertNotNull(formatted);
            assertTrue(formatted.matches("\\d{2}:\\d{2}"),
                    "Phải có định dạng MM:SS, nhận được: " + formatted);
        }

        @Test
        @DisplayName("Trả về '00:00' khi phiên đã hết hạn")
        void shouldReturnZeroFormat_whenExpired() {
            AntiSniper antiSniper = new AntiSniper(-1000L);

            String formatted = antiSniper.getRemainingFormatted();

            assertEquals("00:00", formatted);
        }

        @Test
        @DisplayName("Hiển thị đúng số phút và giây (khoảng 2 phút)")
        void shouldDisplayCorrectMinutesAndSeconds() {
            // 2 phút = 120 000 ms
            AntiSniper antiSniper = new AntiSniper(120_000L);

            String formatted = antiSniper.getRemainingFormatted();

            // Có thể là "01:59" hoặc "02:00" tùy timing; luôn phải match pattern
            assertTrue(formatted.matches("\\d{2}:\\d{2}"));
            String[] parts = formatted.split(":");
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            assertTrue(minutes >= 1 && minutes <= 2,
                    "Phút phải nằm trong khoảng [1, 2], nhận được: " + minutes);
            assertTrue(seconds >= 0 && seconds <= 59,
                    "Giây phải nằm trong khoảng [0, 59], nhận được: " + seconds);
        }
    }
}
