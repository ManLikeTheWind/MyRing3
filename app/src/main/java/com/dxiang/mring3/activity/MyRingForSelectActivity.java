package com.dxiang.mring3.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.gem.imgcash.ImageDownLoader;
import com.dxiang.mring3.R;
import com.dxiang.mring3.bean.ToneInfo;
import com.dxiang.mring3.bean.UserToneGrps;
import com.dxiang.mring3.bean.UserToneGrpsMember;
import com.dxiang.mring3.request.AddRingtoGroupRsquest;
import com.dxiang.mring3.request.DelToneReq;
import com.dxiang.mring3.request.QryUserToneReq;
import com.dxiang.mring3.request.SetToneReq;
import com.dxiang.mring3.response.EntryP;
import com.dxiang.mring3.response.QryUserToneRsp;
import com.dxiang.mring3.utils.Commons;
import com.dxiang.mring3.utils.FusionCode;
import com.dxiang.mring3.utils.LocalMediaPlayer;
import com.dxiang.mring3.utils.UtilLog;
import com.dxiang.mring3.utils.Utils;
import com.dxiang.mring3.utils.UtlisReturnCode;
import com.dxiang.mring3.utils.LocalMediaPlayer.Complete;
import com.dxiang.mring3.utils.LocalMediaPlayer.ErrorListener;
import com.dxiang.mring3.utils.LocalMediaPlayer.onException;
import com.dxiang.mring3.utils.LocalMediaPlayer.onPrepared;
import com.dxiang.mring3.view.RoundedImageView;

public class MyRingForSelectActivity extends BaseActivity {

	private RelativeLayout rl_title_layout;
	// 返回标题
	private TextView mTitle_tv;
	// 编辑
	private TextView tv_edit;
	// 返回图片
	private ImageView mTitle_iv;
	private ListView mToneList;
	private int mToneCount = 0;
	private boolean mRefreshAll = true;
	private int showNum = 100;
	private int pageNo = 0;
	// 上次请求的数据数目，用于判断是否可以再获取更多
	private int mLastDataCount = 0;
	// 铃音集合
	private List<ToneInfo> mToneDataList = new ArrayList<ToneInfo>();
	private LinearLayout mLLNoRing;
	/** 是否在请求数据 **/
	private boolean isLoading = false;
	private LocalMediaPlayer mediaPlayer;
	private boolean isPlayer = false;// 已经在播放
	private ToneAdapter mToneAdapter;
	private PopListAdapter mPopAdapter;
	private String[] objects;
	private int mCurrentPosition = -1;
	private int mTaskCount = 4;
	private ArrayList<String> toneidlist = new ArrayList<String>();
	private ImageView liear_title;
	private String groupid;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_my_ring_library);
		Bundle bundle = getIntent().getExtras();
		toneidlist.clear();
		if (bundle != null) {
			groupid = bundle.getString("groupid");
			toneidlist = bundle.getStringArrayList("groupinfo");
		}
		initView();
		init();
		setListener();
		initListHeader();
		initPopWindow();
		// toneidlist.clear();
		// for (int i = 0; i < grpinfo.getMembersize(); i++) {
		// toneidlist.add(grpinfo.getUserToneGrpsMember(i).getToneID());
		// }
	}

	private void initView() {
		rl_title_layout = (RelativeLayout) findViewById(R.id.rl_title_layout);// 返回字
		mTitle_iv = (ImageView) findViewById(R.id.title_iv);
		mTitle_iv.setOnClickListener(mclick);
		mTitle_tv = (TextView) findViewById(R.id.title_tv);
		tv_edit = (TextView) findViewById(R.id.tv_edit);
		tv_edit.setOnClickListener(mclick);
		mToneList = (ListView) findViewById(R.id.me_ringtone_lv);
		mLLNoRing = (LinearLayout) findViewById(R.id.me_no_ring_ll);
		liear_title = (ImageView) findViewById(R.id.liear_title);
		liear_title.setOnClickListener(mclick);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		initMediaPlayer();
		mToneDataList.clear();
		startPbarU();
		new QryUserToneReq(handler).sendQryUserToneReq(showNum, 0);
	}

	private void init() {
		tv_edit.setVisibility(View.VISIBLE);
		mTitle_iv.setVisibility(View.VISIBLE);
		mTitle_tv.setText(Utils.getResouceStr(mContext,
				R.string.my_ring_library));
		tv_edit.setText(Utils.getResouceStr(mContext,
				R.string.tone_ringgroup_noedit));
	}

	private OnClickListener mclick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.liear_title:
				if (Utils.isFastDoubleClick()) {
					return;
				}
				startActivity(new Intent(MyRingForSelectActivity.this,
						MainGroupActivity.class));
				finish();
				break;
			case R.id.tv_edit:
				toneidlist.clear();
				for (int i = 0; i < mToneDataList.size(); i++) {
					if (isSelected.get(i)) {
						toneidlist.add(mToneDataList.get(i).getToneID());
					}
				}
				if (toneidlist.size() < 1) {
					Utils.showTextToast(mContext,
							getString(R.string.check_least_one));
					return;
				}

				for (int i = 0; i < toneidlist.size(); i++) {
					new AddRingtoGroupRsquest(handler)
							.sendAddRingtoGroupRsquest(
									FusionCode.REQUEST_ADDRINGTOGROUP, "1",
									groupid, toneidlist.get(i));
				}
				break;
			case R.id.title_iv:
				finish();
				break;
			default:
				break;
			}

		}
	};

	/**
	 * 初始化listview的头部和尾部，用来展示主叫被叫彩铃和刷新
	 */
	private void initListHeader() {
		mToneAdapter = new ToneAdapter();
		mToneList.setAdapter(mToneAdapter);
	}

	int deletetag = 0;

	@Override
	protected void reqXmlSucessed(Message msg) {
		// TODO Auto-generated method stub
		//
		super.reqXmlSucessed(msg);
		switch (msg.arg1) {
		// 成功返回个人铃音库，只第一次发起主叫被叫铃音请求
		case FusionCode.REQUEST_ADDRINGTOGROUP:

			if (deletetag >= (toneidlist.size() - 1)) {
				Intent intent = getIntent();
				Bundle bundle = new Bundle();
				bundle.putStringArrayList("checkidlist", toneidlist);
				intent.putExtras(bundle);
				if (intent != null) {
					setResult(RingGroupDetailActivity.ADDNEWTONRETURN, intent);
				}
				finish();
			} else {
				deletetag++;
			}
			stopPbarU();
			break;
		case FusionCode.REQUEST_QRYUSERTONEEVT:
			stopPbarU();
			pageNo++;
			QryUserToneRsp qryUserToneRsp = (QryUserToneRsp) msg.obj;
			List<ToneInfo> mTempInfos = qryUserToneRsp.getToneList();
			mLastDataCount = mTempInfos.size();
			mToneCount = mToneCount + mTempInfos.size();
			mToneDataList.addAll(mTempInfos);
			mToneAdapter.initDate();
			// mToneAdapter.notifyDataSetChanged();
			if (mToneDataList.size() == 0) {
				mToneList.setVisibility(View.GONE);
				mLLNoRing.setVisibility(View.VISIBLE);
				return;
			} else {
				mToneList.setVisibility(View.VISIBLE);
				mLLNoRing.setVisibility(View.GONE);
			}
			if (mRefreshAll) {
				pageNo = 1;
				mRefreshAll = false;
			} else {
				isLoading = false;
				// moreView.setVisibility(View.GONE);
				stopMusic();
				mToneAdapter.notifyDataSetChanged();
			}
			mToneAdapter.notifyDataSetChanged();
			// new SetToneReq(p_h).sendSetToneReq("", "", "", "", "0", "0");
			break;
		default:
			break;
		}
	}

	@Override
	protected void reqXmlFail(Message msg) {
		// TODO Auto-generated method stub
		super.reqXmlFail(msg);
		switch (msg.arg1) {
		case FusionCode.REQUEST_ADDRINGTOGROUP:
			stopPbarU();
			if (deletetag >= (toneidlist.size() - 1)) {
				Intent intent = getIntent();
				Bundle bundle = new Bundle();
				bundle.putStringArrayList("checkidlist", toneidlist);
				intent.putExtras(bundle);
				if (intent != null) {
					setResult(RingGroupDetailActivity.ADDNEWTONRETURN, intent);
				}
				finish();
			} else {
				deletetag++;
			}
			break;
		case FusionCode.REQUEST_QRYUSERTONEEVT:
			stopPbarU();
			if (pageNo == 0) {
				mToneList.setVisibility(View.GONE);
				mLLNoRing.setVisibility(View.VISIBLE);
			}
			if (!mRefreshAll) {
				isLoading = false;
			}
			break;
		}
		EntryP entryP = (EntryP) msg.obj;
		if (!"400020".equals(entryP.getResult())) {
			Utils.showTextToast(mContext,
					UtlisReturnCode.ReturnCode(entryP.getResult(), mContext));
		}
	}

	@Override
	protected void reqError(Message msg) {
		// TODO Auto-generated method stub
		super.reqError(msg);
		switch (msg.arg1) {
		case FusionCode.REQUEST_QRYUSERTONEEVT:
			stopPbarU();
			break;
		}
	}

	/**
	 * 刷新页面，mTaskCount计算任务数量
	 */
	private synchronized void refreshList() {
		mTaskCount--;
		if (mTaskCount == 0) {
			mToneList.setVisibility(View.VISIBLE);
			mToneList.setAdapter(mToneAdapter);
			stopMusic();
			mToneAdapter.notifyDataSetChanged();
			mTaskCount = 4;
			stopPbarU();
			mToneList.setVisibility(View.VISIBLE);
		}
		// initListHeader();
	}

	/**
	 * popwindwo的listview的适配器
	 * 
	 * @author Administrator
	 * 
	 */
	@SuppressLint("ResourceAsColor")
	private class PopListAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return objects.length;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return objects[position];
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@SuppressWarnings("deprecation")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			TextView tv = (TextView) mInflater.inflate(
					R.layout.tonelist_popwindow_list_item, null);
			tv.setText(objects[position]);
			// String language = manager.get("language", "");
			tv.setBackgroundDrawable(getResources().getDrawable(
					R.drawable.list_spain_textview_selector));
			// if(Utils.CheckTextNull(language)){
			// // tv.setText(objects[position]);
			// // tv.setBackgroundResource(R.color.black);
			// // tv.setTextSize(20);
			// }

			return tv;
		}

	}

	private List<String> toneid_listback = new ArrayList<String>();
	private HashMap<Integer, Boolean> isSelected = new HashMap<Integer, Boolean>();

	/**
	 * 铃音库的adapter
	 * 
	 * @author Administrator
	 * 
	 */
	private class ToneAdapter extends BaseAdapter {
		public int pos = -2;
		public int old;

		// 初始化isSelected的数据
		private void initDate() {
			for (int i = 0; i < mToneDataList.size(); i++) {
				getIsSelected().put(i, false);
			}
		}

		@Override
		public int getCount() {
			return mToneDataList.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return mToneDataList.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@SuppressLint("NewApi")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			ToneViewHolder holder = null;
			if (null == convertView) {
				holder = new ToneViewHolder();
				convertView = mInflater.inflate(
						R.layout.activity_ringgroup_tonelistitem, null);
				holder.imagebg = (RoundedImageView) convertView
						.findViewById(R.id.imagebg);
				holder.playIV = (ImageView) convertView
						.findViewById(R.id.music_play_stop_loading);
				holder.songNameTV = (TextView) convertView
						.findViewById(R.id.tone_item_songname_tv);
				holder.songerNameTV = (TextView) convertView
						.findViewById(R.id.tone_item_songername_tv);
				holder.showrightarrow = (ImageView) convertView
						.findViewById(R.id.showrightarrow);
				holder.ck_rember = (CheckBox) convertView
						.findViewById(R.id.ck_rember);
				holder.loadingIV = (ProgressBar) convertView
						.findViewById(R.id.music_loading);
				convertView.setTag(holder);
			} else {
				holder = (ToneViewHolder) convertView.getTag();
			}
			final int temp = position;
			holder.ck_rember.setVisibility(View.VISIBLE);
			holder.showrightarrow.setVisibility(View.GONE);
			ToneInfo toneInfo = mToneDataList.get(position);
			String saveDir = Commons.getImageSavedpath()
					+ toneInfo.getToneClassID();
			if (new File(saveDir).exists()) {
				holder.imagebg.setImageBitmap(ImageDownLoader
						.getShareImageDownLoader().showCacheBitmap(saveDir));
			}
			if (toneidlist.contains(toneInfo.getToneID())) {
				holder.ck_rember.setEnabled(false);
			} else {
				holder.ck_rember.setEnabled(true);
			}
			if (null != toneInfo) {
				holder.songerNameTV.setText(toneInfo.getSingerName());
				holder.songNameTV.setText(toneInfo.getToneName());
			}
			holder.ck_rember.setChecked(getIsSelected().get(position));
			holder.ck_rember.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (getIsSelected().get(temp))
						getIsSelected().put(temp, false);
					else
						getIsSelected().put(temp, true);
				}
			});
			holder.playIV.setOnClickListener(new PlayOnclickListener(temp));
			if (pos == position) {
				if (isPlayer) {
					holder.loadingIV.setVisibility(View.INVISIBLE);
					holder.playIV.setBackgroundResource(R.drawable.music_stop);
				} else {
					holder.playIV.setBackgroundResource(0);
					if (holder.loadingIV.getAnimation() == null) {
						// holder.loadingIV.setAnimation(animation);
						// animation.startNow();
					}
					holder.loadingIV.setVisibility(View.VISIBLE);
				}
			} else {
				holder.loadingIV.setVisibility(View.INVISIBLE);
				holder.playIV.setBackgroundResource(R.drawable.music_play);
			}
			return convertView;
		}

		public HashMap<Integer, Boolean> getIsSelected() {
			return isSelected;
		}

		public void setIsSelected(HashMap<Integer, Boolean> misSelected) {
			isSelected = misSelected;
		}
	}

	/**
	 * 铃音viewholder
	 * 
	 * @author Administrator
	 * 
	 */
	private class ToneViewHolder {
		private RoundedImageView imagebg;
		private CheckBox ck_rember;
		private ImageView showrightarrow;
		private ImageView playIV;
		private ProgressBar loadingIV;
		private TextView songNameTV;
		private TextView songerNameTV;
	}

	/**
	 * 播放暂停按钮的监听器
	 * 
	 * @author Administrator
	 * 
	 */
	private class PlayOnclickListener implements OnClickListener {
		private int temp;

		public PlayOnclickListener(int temp) {
			this.temp = temp;
		}

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			// 防止重复点击
			if (Utils.isFastDoubleClick()) {
				return;
			}
			int oldPos = mToneAdapter.pos;
			mToneAdapter.pos = temp;
			ToneInfo info = null;
			info = mToneDataList.get(temp);
			// 暂停后继续播放
			if (mediaPlayer.isPause() && -1 == oldPos
					&& mToneAdapter.old == temp) {
				mediaPlayer.startPlayer();
				mToneAdapter.pos = temp;
				isPlayer = true;
				return;
			}
			if (oldPos == temp || oldPos == -2 || oldPos == -1) {
				if (oldPos != temp) {
					mediaPlayer.cancelPlayer();
					isPlayer = false;
					mediaPlayer.getUrlFromServer(info.getToneID(),
							info.getToneType());
					mToneAdapter.pos = temp;
					mToneAdapter.notifyDataSetChanged();
					return;
				}
				if (mediaPlayer.isprepared()) {
					return;
				}
				if (!isPlayer) {

					// new
					// GetToneListenAddrReq(p_h).sendGetToneListenAddrReq(toneID,
					// toneType);
					// 发起播放请求
					mediaPlayer.getUrlFromServer(info.getToneID(),
							info.getToneType());
					// mediaPlayer.prepare();
					mToneAdapter.pos = temp;
					mToneList.setStackFromBottom(false);
					mToneAdapter.notifyDataSetChanged();

				} else {
					// 保存暂停的位置
					mToneAdapter.old = mToneAdapter.pos;
					mToneAdapter.pos = -1;
					mToneList.setStackFromBottom(false);
					mToneAdapter.notifyDataSetChanged();
					mediaPlayer.pause();
					isPlayer = false;
				}
			} else {
				mediaPlayer.cancelPlayer();
				isPlayer = false;
				mediaPlayer.setDataSource(info.getTonePreListenAddress());
				mediaPlayer.getUrlFromServer(info.getToneID(),
						info.getToneType());
				mToneAdapter.pos = temp;
				mToneAdapter.notifyDataSetChanged();
			}
		}

	}

	/**
	 * 初始化播放器
	 */
	private void initMediaPlayer() {
		mediaPlayer = LocalMediaPlayer.getInstance();
		mediaPlayer.setCallback(onComplete);
		mediaPlayer.setOnPrepared(onPrepred);
		mediaPlayer.setException(exception);
		mediaPlayer.setErrorListener(errorListener);
	}

	/**
	 * 播放完成
	 */
	Complete onComplete = new Complete() {

		@Override
		public void onComplete() {
			mToneAdapter.pos = -1;
			mToneAdapter.notifyDataSetChanged();
			isPlayer = false;
		}

	};

	/**
	 * 准备播放
	 */
	onPrepared onPrepred = new onPrepared() {

		@Override
		public void onPrepared() {
			if (mToneAdapter.pos != -1 && mToneAdapter.pos != -2) {
				isPlayer = true;
				mToneAdapter.notifyDataSetChanged();

			}
		}
	};

	/**
	 * 播放器出错回调
	 */
	ErrorListener errorListener = new ErrorListener() {

		@Override
		public void onError(MediaPlayer arg0, int arg1, int arg2) {
			stopMusic();
			mToneAdapter.pos = -1;
			mToneAdapter.notifyDataSetChanged();
			isPlayer = false;
		}

	};

	/**
	 * 停止播放
	 */
	private void stopMusic() {
		isPlayer = false;
		mToneAdapter.pos = -1;
		mToneAdapter.notifyDataSetChanged();
		if (mediaPlayer == null) {
			return;
		}
		mediaPlayer.cancelPlayer();
	}

	/**
	 * 播发器的exception
	 */
	onException exception = new onException() {

		@Override
		public void onException(int code, String des) {
			if (Utils.CheckTextNull(des)) {
				Utils.showTextToast(MyRingForSelectActivity.this,
						UtlisReturnCode.ReturnCode(des, mContext));
			}
			stopMusic();
			mToneAdapter.notifyDataSetChanged();
			mToneAdapter.pos = -1;
			isPlayer = false;
		}

	};

	ListView popList;
	// 长按弹出的popwindow
	private PopupWindow mPopWindow;

	/**
	 * 初始化长按弹出的popupwindow
	 */
	private void initPopWindow() {
		// TODO Auto-generated method stub
		View contentView = mInflater.inflate(
				R.layout.tonelist_popwindow_layout, null);
		objects = getResources()
				.getStringArray(R.array.pop_caller_crbt_strArry);
		popList = (ListView) contentView
				.findViewById(R.id.tonelist_popwindow_lv);

		mPopAdapter = new PopListAdapter();
		popList.setAdapter(mPopAdapter);
		mPopWindow = new PopupWindow(contentView, LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT, true);
		mPopWindow.setBackgroundDrawable(new BitmapDrawable());
		mPopWindow.setOutsideTouchable(true);

		popList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				TextView tv = (TextView) view;
				String itemname = tv.getText().toString();
				//
				String string = getString(R.string.me_pop_delete);
				// 删除铃音
				if (getString(R.string.me_pop_delete).equals(itemname)) {
					ToneInfo info = mToneDataList.get(mCurrentPosition);
					if (null != info) {
						String toneType = info.getToneType();
						String toneID = info.getToneID();
						startPbarU();
						new DelToneReq(handler)
								.sendDelToneReq(toneType, toneID);
					}

				}
				// 设置主叫
				if (getString(R.string.me_pop_set_caller).equals(itemname)) {
					ToneInfo info = mToneDataList.get(mCurrentPosition);
					if (null != info) {
						String toneID = info.getToneID();
						// String toneType = info.getToneType();
						String operType = "1";
						String settingID = "0000000000";
						String serviceType = "2";
						startPbarU();
						new SetToneReq(handler).sendSetToneReq(operType,
								serviceType, settingID, toneID, "0", "0");
					}

				}
				// 设置被叫
				if (getString(R.string.me_pop_set_called).equals(itemname)) {
					ToneInfo info = mToneDataList.get(mCurrentPosition);
					if (null != info) {
						String toneID = info.getToneID();
						// String toneType = info.getToneType();
						String operType = "1";
						String settingID = "0000000000";
						String serviceType = "1";
						startPbarU();
						new SetToneReq(handler).sendSetToneReq(operType,
								serviceType, settingID, toneID, "0", "0");
					}

				}
				mPopWindow.dismiss();

			}
		});
	}

	private void setListener() {
		mToneList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				// 点击时间过快，不操作
				if (Utils.isFastDoubleClick()) {
					return;
				}
				mCurrentPosition = position;
				objects = getResources().getStringArray(
						R.array.pop_listitem_strArry);
				mPopWindow.showAtLocation(mToneList, Gravity.BOTTOM, 0, 0);
				mPopAdapter.notifyDataSetChanged();
			}
		});

		mToneList.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {

				// mPopAdapter.notifyDataSetChanged();
				return true;
			}
		});
	}

	@Override
	public void onStop() {
		super.onStop();
		if (mediaPlayer == null) {
			return;
		}
		// 如果正在播放就暂停，否则停止
		if (mediaPlayer.isPlaying()
				|| mediaPlayer.getState() == LocalMediaPlayer.PAUSE) {
			pause();
		} else {
			stopMusic();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mediaPlayer != null) {
			mediaPlayer.uninsMedia();
			mediaPlayer = null;
		}
	}

	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if (mediaPlayer == null) {
			return;
		}
		if (mediaPlayer.isPlaying()
				|| mediaPlayer.getState() == LocalMediaPlayer.PAUSE) {
			pause();
		} else {
			stopMusic();
		}
	}

	private void pause() {
		if (mediaPlayer.getState() != LocalMediaPlayer.PAUSE) {
			mToneAdapter.old = mToneAdapter.pos;
			mToneAdapter.pos = -1;
			mToneAdapter.notifyDataSetChanged();
			mediaPlayer.pause();
			isPlayer = false;
		}
	}

}
