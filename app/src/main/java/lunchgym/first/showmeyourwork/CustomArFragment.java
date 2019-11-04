package lunchgym.first.showmeyourwork;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ux.ArFragment;

public class CustomArFragment extends ArFragment {


    @Override
    protected Config getSessionConfiguration(Session session) {

        Config config = new Config(session);
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
        config.setFocusMode(Config.FocusMode.AUTO);

        //AR 이미지 데이터 베이스
        AugmentedImageDatabase aid = new AugmentedImageDatabase(session);


        //AR 이미지 데이터 베이스에 예쁜 아이린 이미지(테스트용)
        Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.cleaner);
        //이미지 데이터베이스에 넣을때 키워드도 같이 들어가네
        aid.addImage("image", image);

        //데이터 베이스를 다시 config 에 설정해주고 리턴해주네
        config.setAugmentedImageDatabase(aid);

        this.getArSceneView().setupSession(session);

        return config;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        FrameLayout frameLayout = (FrameLayout) super.onCreateView(inflater, container, savedInstanceState);

        //이거 안 숨기면 처음에 듀토리얼 같이 손 나옴
        getPlaneDiscoveryController().hide();
        getPlaneDiscoveryController().setInstructionView(null);

        return frameLayout;


    }
}
