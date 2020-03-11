package com.lingyi.autiovideo.test.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.bnc.activity.T01Helper;
import com.bnc.activity.engine.MeetingEngine;
import com.bnc.activity.entity.ConferenceEntity;
import com.bnc.activity.entity.PttHistoryEntity;
import com.bnc.activity.view.manager.MeetingManager;
import com.google.gson.Gson;
import com.lingyi.autiovideo.test.Constants;
import com.lingyi.autiovideo.test.R;
import com.lingyi.autiovideo.test.adapter.MeetingMemberControlAdapter;
import com.lingyi.autiovideo.test.model.MeetingMemberControlEntity;
import com.lingyi.autiovideo.test.model.VoipContactEntity;
import com.lingyi.autiovideo.test.utils.JsonUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AudioCallActivity extends Activity {

    private boolean isHandfree = false;
    private boolean isMute = false;
    private AudioCallReceiver mCllReceiver;
    private TextView mName;
    private MeetingMemberControlAdapter meetingMemberControlAdapter;
    private String mMeetingID;



    private ExecutorService executorService = new ThreadPoolExecutor(10, 10,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);


        mName = findViewById(R.id.tv_name);


        mName.setText(getNumber());

        mCllReceiver = new AudioCallReceiver();

        mMeetingID = getMeetingId();
        if (!TextUtils.isEmpty(mMeetingID)) {
            meetingMemberControlAdapter = new MeetingMemberControlAdapter(null);
            mRecyclerView = findViewById(R.id.recycler);
            configRecyclerView(mRecyclerView, new GridLayoutManager(this, 3));
            mRecyclerView.setAdapter(meetingMemberControlAdapter);
            mName.setText("会议:" + mMeetingID);
            meetingMemberControlAdapter.setNewData(getData());
            findViewById(R.id.join).setVisibility(View.VISIBLE);
        }
        registerReceiver();
        initListener();
    }

    private void initListener() {
        findViewById(R.id.handfree).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isHandfree = !isHandfree;
                T01Helper.getInstance().getCallEngine().isHandsfree(isHandfree);

            }
        });


        findViewById(R.id.mute).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isMute = !isMute;
                T01Helper.getInstance().getCallEngine().isMute(isMute);
            }
        });
        ;

        findViewById(R.id.join).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AudioCallActivity.this, CreateGroupActivity.class);
                intent.putExtra(getString(R.string.Select_User), 3);
                startActivityForResult(intent, 2);
            }
        });


        findViewById(R.id.hangup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                T01Helper.getInstance().getCallEngine().hangUpCall();
                finish();
            }
        });

        findViewById(R.id.btn_all_mute).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestMeetingMuteHandle(-1, null);
            }
        });


        if (meetingMemberControlAdapter != null) {
            meetingMemberControlAdapter.addChildClickListener(new MeetingMemberControlAdapter.IMeetingControlListener() {
                @Override
                public void onMute(int adapterPosition, VoipContactEntity voipContactEntity) {
                    requestMeetingMuteHandle(adapterPosition, voipContactEntity);
                }

                @Override
                public void onDel(int adapterPosition, VoipContactEntity voipContactEntity) {
                    requestMeetingDelHandle(adapterPosition, voipContactEntity);
                }
            });
        }

        T01Helper.getInstance().getMeetingEngine().addMeetingStateListener(mMeetingID, 3000, new MeetingManager.IMeetingStateListener() {
            @Override
            public void onError(String s) {

            }

            @Override
            public void onData(final String json) {
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        MeetingMemberControlEntity meetingMemberControlEntity = JsonUtil.json2Bean(json, MeetingMemberControlEntity.class);
                        upDateMeetingMember(meetingMemberControlEntity);


                    }
                });
            }
        });
    }


    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            meetingMemberControlAdapter.notifyDataSetChanged();
        }
    };

    /**
     * 更新组内成员
     *
     * @param conferenceEntity
     */
    private void upDateMeetingMember(MeetingMemberControlEntity conferenceEntity) {
        if (conferenceEntity != null && meetingMemberControlAdapter.getData() != null && meetingMemberControlAdapter.getData().size() > 0) {
            if (conferenceEntity != null
                    && conferenceEntity.getConferences() != null
                    && conferenceEntity.getConferences().getConference() != null
                    && conferenceEntity.getConferences().getConference().getMembers() != null
                    && conferenceEntity.getConferences().getConference().getMembers().getMember() != null
                    && conferenceEntity.getConferences().getConference().getMembers().getMember().size() > 0) {

                for (MeetingMemberControlEntity.ConferencesBean.ConferenceBean.MembersBean.MemberBean memberBean :
                        conferenceEntity.getConferences().getConference().getMembers().getMember()) {
                    if (memberBean == null || memberBean.getFlags() == null) continue;
                    //当前 userId
                    String caller_id_number = memberBean.getCaller_id_number();
                    //当前 userName
                    String caller_id_name = memberBean.getCaller_id_name();
                    //当前 操作ID
                    String id = memberBean.getId();
                    //talking :true/false
                    String talking = memberBean.getFlags().getTalking();

                    if (TextUtils.isEmpty(caller_id_number) || !TextUtils.isEmpty(caller_id_name) ||
                            TextUtils.isEmpty(id) || TextUtils.isEmpty(talking)) {
                        upData(caller_id_number, caller_id_name, id, talking);
                    }
                }
            }
        }
    }

    private void upData(String caller_id_number, String caller_id_name, String id, String talking) {
        if (meetingMemberControlAdapter != null && meetingMemberControlAdapter.getData().size() > 0) {
            for (VoipContactEntity datum : meetingMemberControlAdapter.getData()) {
                if (caller_id_number.equals(datum.getNumber())) {
                    boolean aTrue = talking.equals("true") ? true : false;
                    if (datum.isTalk() == aTrue) continue;//如果状态一样，不要更新
                    datum.setMeetingMemberId(id);
                    datum.setName(caller_id_name);
                    datum.setTalk(aTrue);
                    updata();
                }
            }
        }
    }
    private void updata() {
        runOnUiThread(runnable);
    }

    /**
     * 请求会议成员删除
     *
     * @param voipContactEntity
     */
    private void requestMeetingDelHandle(final int postion, VoipContactEntity voipContactEntity) {
        if (!TextUtils.isEmpty(mMeetingID) && !TextUtils.isEmpty(voipContactEntity.getMeetingMemberId())) {
            T01Helper.getInstance().getMeetingEngine().delMeetingMember(mMeetingID, Integer.valueOf(voipContactEntity.getMeetingMemberId()), new MeetingManager.IMeetingDelListener() {
                @Override
                public void onError(String s) {
                    ToastUtils.showShort("删除失败:" + s);
                }

                @Override
                public void onData(String str) {
                    ToastUtils.showShort("操作成功:" + str);
                    meetingMemberControlAdapter.remove(postion);
                    meetingMemberControlAdapter.notifyItemChanged(postion);
                }
            });
        }

    }

    /**
     * 请求会议成员禁言处理
     *
     * @param voipContactEntity
     */
    boolean isALLMute;

    private void requestMeetingMuteHandle(final int postion, final VoipContactEntity voipContactEntity) {
        int number;
        if (postion == -1 && null == voipContactEntity) {
            isMute = !isALLMute;
            number = MeetingEngine.MeetingHandle.ALL.ordinal();
        } else {
            isMute = !voipContactEntity.isSelect();
            if (TextUtils.isEmpty(voipContactEntity.getMeetingMemberId())) return;
            number = Integer.valueOf(voipContactEntity.getMeetingMemberId());

        }
        if (!TextUtils.isEmpty(mMeetingID)) {
            T01Helper.getInstance().getMeetingEngine().meetingMemberMuteHandle(isMute, mMeetingID, number, new MeetingManager.IMeetingMemberMuteListener() {
                @Override
                public void onError(String s) {
                    ToastUtils.showShort("禁言失败:" + s);
                }

                @Override
                public void onData(String s) {
                    ToastUtils.showShort("禁言成功:" + s);
                    if (postion == -1 && null == voipContactEntity) {
                        for (VoipContactEntity datum : meetingMemberControlAdapter.getData()) {//全部禁言或者取消禁言
                            datum.setSelect(isMute);
                        }
                        meetingMemberControlAdapter.notifyDataSetChanged();
                    } else {
                        meetingMemberControlAdapter.getData().get(postion).setSelect(!voipContactEntity.isSelect());//指定禁言或者取消禁言
                        meetingMemberControlAdapter.notifyItemChanged(postion);
                    }

                }
            });
        }
    }


    /**
     * 配置 RecyclerView
     *
     * @param recyclerView
     * @param layoutManager
     */
    public static void configRecyclerView(final RecyclerView recyclerView
            , RecyclerView.LayoutManager layoutManager) {
        recyclerView.setLayoutManager(layoutManager);
        //如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }


    private String getMeetingId() {
        return getIntent().getStringExtra(Constants.CALL_MEETING_ID);
    }

    private List<VoipContactEntity> getData() {
        ArrayList<VoipContactEntity> voipContactEntities = new ArrayList<>();
        if (!getNumber().isEmpty()) {
            if (getNumber().contains(",")) {
                String[] numbers = getNumber().split(",");
                for (int i = 0; i < numbers.length; i++) {
                    String str = numbers[i];
                    voipContactEntities.add(new VoipContactEntity(str, str, str));
                }
            } else
                voipContactEntities.add(new VoipContactEntity(getNumber(), getNumber(), getNumber()));

        }
        return voipContactEntities;
    }

    private String getNumber() {
        if (getIntent() != null && getIntent().getStringExtra(Constants.CALL_NUMBER) != null) {
            return getIntent().getStringExtra(Constants.CALL_NUMBER);
        }
        return "";
    }

    @Override
    protected void onDestroy() {
        if (!TextUtils.isEmpty(mMeetingID))
            T01Helper.getInstance().getMeetingEngine().stopMeeting(mMeetingID, new MeetingManager.IMeetingStopListener() {
                @Override
                public void onError(String s) {

                }

                @Override
                public void onData(String s) {
                }
            });
        super.onDestroy();
        if (mCllReceiver != null) {
            unregisterReceiver(mCllReceiver);
            mCllReceiver = null;
        }


    }

    public void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.CALL_ACION);
        intentFilter.addAction(Constants.CALL_IN_OR_OUR_ACION);
        registerReceiver(mCllReceiver, intentFilter);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        T01Helper.getInstance().getCallEngine().hangUpCall();
        finish();
    }

    private class AudioCallReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction().equals(Constants.CALL_ACION)) {
                T01Helper.getInstance().getCallEngine().hangUpCall();
                finish();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2 && resultCode == 2 && data != null && data.getParcelableArrayListExtra("JOIN_MEMBER") != null) {
            ArrayList<VoipContactEntity> join_member = data.getParcelableArrayListExtra("JOIN_MEMBER");
            Iterator<VoipContactEntity> iterator = join_member.iterator();
            while (iterator.hasNext()) {
                VoipContactEntity cur = iterator.next();
                for (VoipContactEntity datum : meetingMemberControlAdapter.getData()) {
                    if (datum.getNumber().equals(cur.getNumber())) {
                        iterator.remove();
                        break;
                    }
                }
            }

            requestJoinMeeting(join_member);


        }

    }


    /**
     * 邀请加入会议
     *
     * @param joinMember
     */
    private void requestJoinMeeting(final ArrayList<VoipContactEntity> joinMember) {
        String members = getJoinMember(joinMember);
        if (members != null && !members.isEmpty() && mMeetingID != null && !mMeetingID.isEmpty()) {
            T01Helper.getInstance().getMeetingEngine().joinMeeting(mMeetingID, members, new MeetingManager.IMeetingJoinListener() {
                @Override
                public void onError(String eror) {
                    Log.i("requestJoinMeeting", eror);
                    ToastUtils.showShort("邀请失败:" + eror);
                }

                @Override
                public void onData(String json) {
                    Log.i("requestJoinMeeting", json);
                    meetingMemberControlAdapter.addData(joinMember);
                    meetingMemberControlAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private String getJoinMember(ArrayList<VoipContactEntity> join_member) {
        StringBuilder sb = new StringBuilder();
        if (join_member.size() > 1) {
            for (VoipContactEntity voipContactEntity : join_member) {
                sb.append(voipContactEntity.getNumber()).append(",");
            }
            return sb.toString();

        }
        return join_member.size() == 0 ? "" : join_member.get(0) == null ? "" : join_member.get(0).getNumber();
    }


}
