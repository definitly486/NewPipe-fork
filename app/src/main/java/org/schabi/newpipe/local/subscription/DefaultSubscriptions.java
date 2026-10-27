package org.schabi.newpipe.local.subscription;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.schabi.newpipe.NewPipeDatabase;
import org.schabi.newpipe.database.subscription.SubscriptionDAO;
import org.schabi.newpipe.database.subscription.SubscriptionEntity;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.channel.ChannelInfo;
import org.schabi.newpipe.util.image.ImageStrategy;

import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Встроенный список подписок. Добавляет только те каналы, которых ещё нет в базе,
 * поэтому безопасно вызывать при каждом запуске приложения.
 * <p>
 * После вставки для каналов без аватарки (например, после новой установки)
 * подгружается информация о канале, чтобы в списке подписок были настоящие иконки.
 * Если сети нет, попытка повторится при следующем запуске.
 */
public final class DefaultSubscriptions {
    private static final String TAG = "DefaultSubscriptions";
    private static final int YOUTUBE_SERVICE_ID = 0;

    // {url, name}
    private static final String[][] CHANNELS = {
        {"https://www.youtube.com/channel/UCjCNdxBqo7HZLMo9j9zNkjg", "-ТАЁЖНЫЙ-"},
        {"https://www.youtube.com/channel/UCY03gpyR__MuJtBpoSyIGnw", "Droider"},
        {"https://www.youtube.com/channel/UC9QCSnXY8JR32jYNUVU8rgA",
            "Solo wild hikes in Siberia from ABVGAT. Survival."},
        {"https://www.youtube.com/channel/UCdubelOloxR3wzwJG9x8YqQ", "TV Rain"},
        {"https://www.youtube.com/channel/UCBpeYNfLtLMuxBYeQTeeuog", "Всё из за Метало Копа"},
        {"https://www.youtube.com/channel/UCuUH2_MlIpWpJnWZKeyVGrQ", "ЗА ВЕРСТУ"},
        {"https://www.youtube.com/channel/UCgpSieplNxXxLXYAzJLLpng", "Майкл Наки"},
        {"https://www.youtube.com/channel/UC5bhU-LtufD3tl2C9OyAzkw", "НАЕДИНЕ С ТАЙГОЙ"},
        {"https://www.youtube.com/channel/UCRGiaVdWE7nvjH9Ak8-oIPw", "Реки Севера. Northern Rivers"},
        {"https://www.youtube.com/channel/UCk94O116OXhPAOWZM7S2Leg", "Ремонтяш"},
        {"https://www.youtube.com/channel/UC9Omlbb17r_o7WwQsyvGV-A", "Руслан Шемякин"},
        {"https://www.youtube.com/channel/UCX-Yk13ftllhgSt6FrOrO8A", "hackmyth"},
    };

    private DefaultSubscriptions() { }

    public static void insertIfMissing(@NonNull final Context context) {
        Schedulers.io().scheduleDirect(() -> {
            final SubscriptionDAO dao = NewPipeDatabase.getInstance(context).subscriptionDAO();
            for (final String[] channel : CHANNELS) {
                final boolean missing = dao.getSubscription(YOUTUBE_SERVICE_ID, channel[0])
                        .isEmpty()
                        .blockingGet();
                if (missing) {
                    final SubscriptionEntity entity = new SubscriptionEntity();
                    entity.setServiceId(YOUTUBE_SERVICE_ID);
                    entity.setUrl(channel[0]);
                    entity.setName(channel[1]);
                    dao.insert(entity);
                }
            }
            refreshMissingAvatars(dao);
        });
    }

    /**
     * Для встроенных каналов, у которых в базе нет аватарки, загружает информацию о канале
     * (имя, аватарка, описание, число подписчиков) и сохраняет её в базу.
     * Ошибки по одному каналу не мешают обновлению остальных.
     */
    private static void refreshMissingAvatars(@NonNull final SubscriptionDAO dao) {
        for (final String[] channel : CHANNELS) {
            try {
                final SubscriptionEntity entity =
                        dao.getSubscription(YOUTUBE_SERVICE_ID, channel[0]).blockingGet();
                if (entity == null) {
                    continue;
                }
                final String avatarUrl = entity.getAvatarUrl();
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    continue; // иконка уже есть
                }

                final ChannelInfo info = ChannelInfo.getInfo(
                        NewPipe.getService(YOUTUBE_SERVICE_ID), channel[0]);
                entity.setName(info.getName());
                entity.setAvatarUrl(ImageStrategy.imageListToDbUrl(info.getAvatars()));
                entity.setDescription(info.getDescription());
                entity.setSubscriberCount(info.getSubscriberCount());
                dao.update(entity);
            } catch (final Exception e) {
                // нет сети или YouTube недоступен: повторим при следующем запуске
                Log.w(TAG, "Could not refresh avatar for " + channel[0], e);
            }
        }
    }
}
