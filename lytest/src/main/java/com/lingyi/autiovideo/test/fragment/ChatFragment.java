package com.lingyi.autiovideo.test.fragment;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.blankj.utilcode.util.ToastUtils;
import com.bnc.activity.PttApplication;
import com.bnc.activity.T01Helper;
import com.bnc.activity.callback.IRecvMessageListener;
import com.bnc.activity.entity.MsgMessageEntity;
import com.bnc.activity.utils.MsgUtil;
import com.lingyi.autiovideo.test.R;
import com.lingyi.autiovideo.test.adapter.ChatAdapter;

import java.util.ArrayList;

/**
 * <pre>
 *     author  : devyk on 2019-09-04 14:56
 *     blog    : https://juejin.im/user/578259398ac2470061f3a3fb/posts
 *     github  : https://github.com/yangkun19921001
 *     mailbox : yang1001yk@gmail.com
 *     desc    : This is ChatFragment
 * </pre>
 */
public class ChatFragment extends BaseFragment implements View.OnClickListener {


    private static ChatFragment chatFragment;
    private EditText editText, et_target;
    private RecyclerView recyclerView;
    private Button btnMessage, btn_save;
    private ChatAdapter chatAdapter;

    ArrayList<String> messageEntityArrayList = new ArrayList<>();
    private String targetNumber;

    public static ChatFragment getInstance() {
        chatFragment = new ChatFragment();
        return chatFragment;
    }


    @Override
    protected void initData() {
        recyclerView = mView.findViewById(R.id.rlv);
        editText = mView.findViewById(R.id.et_meg);
        et_target = mView.findViewById(R.id.et_target);
        btnMessage = mView.findViewById(R.id.btn_send_message);
        btn_save = mView.findViewById(R.id.btn_save);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chatAdapter = new ChatAdapter(messageEntityArrayList);
        recyclerView.setAdapter(chatAdapter);

    }

    @Override
    protected void initListener() {
        btnMessage.setOnClickListener(this);
        btn_save.setOnClickListener(this);

    }

    @Override
    public int getLayout() {
        return R.layout.fragment_chat;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send_message:
                if (editText.getText().toString().trim().isEmpty()) {
                    ToastUtils.showShort("输入信息。");
                    return;
                }
                if (et_target.getText().toString().trim().isEmpty()) {
                    ToastUtils.showShort("输入目标ID");
                    return;
                }

                chatAdapter.addData(editText.getText().toString().trim());
                recyclerView.smoothScrollToPosition(chatAdapter.getData().size() - 1);
                chatAdapter.notifyDataSetChanged();
                sendMessage(editText.getText().toString().trim());
                break;
            case R.id.btn_save:
                save(et_target.getText().toString().trim());
                break;
        }

    }

    /**
     * 开始根据号码设置监听
     *
     * @param targetNumber
     */
    private void save(String targetNumber) {
        if (targetNumber.isEmpty()) {
            ToastUtils.showShort("输入目标ID");
            return;
        }
        //加载当前对话的默认聊天内容
        T01Helper.getInstance().getMessageEngine().loadDefaultMeg(1, 100, Integer.parseInt(targetNumber));

        /**
         * 当前发送消息的监听
         */
        T01Helper.getInstance().getMessageEngine().recvMessageListener(new IRecvMessageListener() {
            @Override
            public void setSEND_MSG_ERROR(String uniqueID, int reason) {
                Log.i(TAG, "失败");
                ToastUtils.showShort("发送失败");
                editText.setText("");
            }

            @Override
            public void setSEND_MSG_SUCCEED(String uniqueID) {
                Log.i(TAG, "成功");
                ToastUtils.showShort("发送成功");
                editText.setText("");
            }

            /**
             * 当前接收到的消息
             * @param msgMessageEntity
             */
            @Override
            public void getCurrentRevMeg(MsgMessageEntity msgMessageEntity) {
                Log.i(TAG, msgMessageEntity.toString());
                chatAdapter.addData(msgMessageEntity.getMessageContent());
                chatAdapter.notifyDataSetChanged();
            }

            @Override
            public void getUserIsOnline(boolean b, boolean b1) {

            }

            /**
             * 第一次打开页面收到的历史消息
             * @param arrayList
             * @param i
             */
            @Override
            public void getAllCurrentMeg(ArrayList<MsgMessageEntity> arrayList, int i) {
                Log.i(TAG, "getAllCurrentMeg--->" + arrayList.size());
                for (MsgMessageEntity msgMessageEntity : arrayList) {
                    chatAdapter.addData(msgMessageEntity.getMessageContent());
                }
            }

            @Override
            public void getMoreMeg(ArrayList<MsgMessageEntity> arrayList, int i) {
                Log.i(TAG, "getMoreMeg--->" + arrayList.size());

            }
        }, 1, Integer.parseInt(targetNumber));

    }

    private void sendMessage(String message) {
        sendMessage(MsgUtil.IMsgType.TXT, message,
                PttApplication.getInstance().getUserId(), "发送者", Integer.parseInt(et_target.getText().toString().trim()), "接收者", null, 1, null, null);
    }

    /**
     * @param messageType    @see MsgUtil.IMsgType.TXT --> 0 :txt,1:视频,3:图片,4:录音,10:文件,
     * @param sendContent    发送的内容
     * @param sendPoliceId   发送者 ID
     * @param sendPoliceName 发送者姓名
     * @param recverID       对方 ID
     * @param recverName     对方姓名
     * @param file           发送的文件
     * @param type           1:单聊 ，2 群聊
     * @param sendVoicelong  语音长度
     * @param uuid           消息唯一 ID
     */
    public void sendMessage(int messageType, String sendContent, String sendPoliceId, String sendPoliceName, int recverID,
                            String recverName, String file, int type, String sendVoicelong, String uuid) {
        T01Helper.getInstance().getMessageEngine().sendMessage(messageType, sendContent, sendPoliceId, sendPoliceName, recverID, recverName, file, type, sendVoicelong, uuid);
    }
}