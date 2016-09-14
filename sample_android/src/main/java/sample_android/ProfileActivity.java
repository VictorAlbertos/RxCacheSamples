package sample_android;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.squareup.picasso.Picasso;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.rx_cache2.Reply;
import sample_data.entities.User;
import victoralbertos.io.rxjavacache.R;

/**
 * Created by victor on 09/01/16.
 */
public class ProfileActivity extends BaseActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);
        getUserLogged(false);
    }

    private void showLogin() {
        findViewById(R.id.ll_logged).setVisibility(View.GONE);
        findViewById(R.id.ll_login).setVisibility(View.VISIBLE);

        findViewById(R.id.bt_login).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String userName = ((EditText) findViewById(R.id.et_name)).getText().toString();

                disposable.dispose();
                disposable = getRepository().loginUser(userName)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<Reply<User>>() {
                          @Override public void accept(Reply<User> userReply) throws Exception {
                            showLogged(userReply);
                          }
                        }, new Consumer<Throwable>() {
                          @Override public void accept(Throwable e) throws Exception {
                            Toast.makeText(ProfileActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                          }
                        });
            }
        });
    }

    private void showLogged(Reply<User> userReply) {
        findViewById(R.id.ll_login).setVisibility(View.GONE);
        findViewById(R.id.ll_logged).setVisibility(View.VISIBLE);

        User user = userReply.getData();

        Picasso.with(this).load(user.getAvatarUrl())
                .centerCrop()
                .fit()
                .into((ImageView) findViewById(R.id.iv_avatar));

        ((TextView)findViewById(R.id.tv_name)).setText(user.getLogin());
        ((TextView)findViewById(R.id.tv_source)).setText("Loaded from: " + userReply.getSource().name());

        findViewById(R.id.bt_refresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getUserLogged(true);
            }
        });

        findViewById(R.id.bt_logout).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
              disposable.dispose();
              disposable = getRepository().logoutUser()
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<String>() {
                          @Override public void accept(String feedback) throws Exception {
                            Toast.makeText(ProfileActivity.this, feedback, Toast.LENGTH_LONG).show();
                            showLogin();
                          }
                        });
            }
        });
    }

    private void getUserLogged(boolean update) {
      disposable.dispose();
      disposable = getRepository().getLoggedUser(update)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Reply<User>>() {
                  @Override public void accept(Reply<User> userReply) throws Exception {
                    showLogged(userReply);
                  }
                }, new Consumer<Throwable>() {
                  @Override public void accept(Throwable e) throws Exception {
                    Toast.makeText(ProfileActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                  }
                });
    }
}
