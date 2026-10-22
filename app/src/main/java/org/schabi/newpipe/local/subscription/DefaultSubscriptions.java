package org.schabi.newpipe.local.subscription;

import android.content.Context;

import androidx.annotation.NonNull;

import org.schabi.newpipe.NewPipeDatabase;
import org.schabi.newpipe.database.subscription.SubscriptionDAO;
import org.schabi.newpipe.database.subscription.SubscriptionEntity;

import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Встроенный список подписок. Добавляет только те каналы, которых ещё нет в базе,
 * поэтому безопасно вызывать при каждом запуске приложения.
 */
public final class DefaultSubscriptions {
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
        });
    }
}
