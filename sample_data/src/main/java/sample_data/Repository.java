package sample_data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import io.rx_cache2.DynamicKey;
import io.rx_cache2.EvictDynamicKey;
import io.rx_cache2.EvictProvider;
import io.rx_cache2.ProviderHelper;
import io.rx_cache2.Reply;
import io.rx_cache2.internal.RxCache;
import io.victoralbertos.jolyglot.GsonSpeaker;
import java.io.File;
import java.io.IOException;
import java.util.List;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import sample_data.cache.CacheProviders;
import sample_data.entities.Repo;
import sample_data.entities.User;
import sample_data.net.RestApi;

public class Repository {
    public static final int USERS_PER_PAGE = 25;

    public static Repository init(File cacheDir) {
        return new Repository(cacheDir);
    }

    private final CacheProviders cacheProviders;
    private final RestApi restApi;

    public Repository(File cacheDir) {
        cacheProviders = new RxCache.Builder()
                .persistence(cacheDir, new GsonSpeaker())
                .using(CacheProviders.class);

        restApi = new Retrofit.Builder()
                .baseUrl(RestApi.URL_BASE)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(RestApi.class);
    }

    public Observable<Reply<List<User>>> getUsers(int idLastUserQueried, final boolean update) {
        return cacheProviders.getUsers(restApi.getUsers(idLastUserQueried, USERS_PER_PAGE), new DynamicKey(idLastUserQueried), new EvictDynamicKey(update));
    }

    public Observable<Reply<List<Repo>>> getRepos(final String userName, final boolean update) {
        return cacheProviders.getRepos(restApi.getRepos(userName), new DynamicKey(userName), new EvictDynamicKey(update));
    }

    public Observable<Reply<User>> loginUser(final String userName) {
        return restApi.getUser(userName).flatMap(
            new Function<Response<User>, ObservableSource<Reply<User>>>() {
                @Override public ObservableSource<Reply<User>> apply(Response<User> userResponse)
                    throws Exception {
                    if (!userResponse.isSuccessful()) {
                        try {
                            ResponseError responseError = new Gson().fromJson(userResponse.errorBody().string(), ResponseError.class);
                            throw new RuntimeException(responseError.getMessage());
                        } catch (JsonParseException | IOException exception) {
                            throw new RuntimeException(exception.getMessage());
                        }
                    }

                    return cacheProviders.getCurrentUser(Observable.just(userResponse.body()), new EvictProvider(true));
                }
            });
    }

    public Observable<String> logoutUser() {
        return cacheProviders.getCurrentUser(ProviderHelper.<User>withoutLoader(), new EvictProvider(true))
                .map(new Function<Reply<User>, String>() {
                    @Override public String apply(Reply<User> user) throws Exception {
                        return "Logout";
                    }
                })
                .onErrorReturn(new Function<Throwable, String>() {
                    @Override public String apply(Throwable throwable) {
                        return "Logout";
                    }
                });
    }

    public Observable<Reply<User>> getLoggedUser(boolean invalidate) {
        Observable<Reply<User>> cachedUser = cacheProviders.getCurrentUser(ProviderHelper.<User>withoutLoader(), new EvictProvider(false));

        Observable<Reply<User>> freshUser = cachedUser.flatMap(new Function<Reply<User>, ObservableSource<Reply<User>>>() {
            @Override public ObservableSource<Reply<User>> apply(Reply<User> userReply) throws Exception {
                return loginUser(userReply.getData().getLogin());
            }
        });

        if (invalidate) return freshUser;
        else return cachedUser;
    }

    private static class ResponseError {
        private final String message;

        public ResponseError(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
