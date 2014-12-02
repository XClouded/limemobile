package com.limemobile.app.sdk.orm;

import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.limemobile.app.sdk.http.BasicJSONResponse;
import com.limemobile.app.sdk.http.HttpUtils;
import com.limemobile.app.sdk.http.JSONResponseListener;
import com.limemobile.app.sdk.http.volley.VolleyClient;
import com.limemobile.app.sdk.http.volley.VolleyClientRequest;
import com.limemobile.app.sdk.orm.gson.GsonModel;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.AbstractDaoMaster;
import de.greenrobot.dao.AbstractDaoSession;
import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.dao.query.WhereCondition;

public class VolleyModelProvider<T> {
    public static final int QUERY_SUCCESS_MESSAGE = 0;
    public static final int QUERY_FAILURE_MESSAGE = 1;
    public static final int QUERY_START_MESSAGE = 2;
    public static final int QUERY_FINISH_MESSAGE = 3;
    public static final int QUERY_CANCEL_MESSAGE = 6;
    public static final int UPDATE_SUCCESS_MESSAGE = 7;
    public static final int UPDATE_FAILURE_MESSAGE = 8;
    public static final int UPDATE_START_MESSAGE = 9;
    public static final int UPDATE_FINISH_MESSAGE = 10;
    public static final int UPDATE_CANCEL_MESSAGE = 12;

    protected final Context mContext;

    protected final SQLiteDatabase mDatabase;
    protected final AbstractDaoMaster mDaoMaster;
    protected final AbstractDaoSession mDaoSession;
    protected final AbstractDao<T, Long> mDao;

    protected final VolleyClient mHttpClient;

    protected final Handler mHandler;

    protected final GsonModel<T> mGsonModel;

    protected final ModelProviderListener<T> mListener;

    @SuppressWarnings("unchecked")
    public VolleyModelProvider(Context context, SQLiteDatabase db,
            AbstractDaoMaster daoMaster, Class<T> clazz, Looper looper,
            GsonModel<T> gson, ModelProviderListener<T> listener) {
        super();
        mContext = context;

        mDatabase = db;
        mDaoMaster = daoMaster;
        mDaoSession = mDaoMaster.newSession();
        mDao = (AbstractDao<T, Long>) mDaoSession.getDao(clazz);

        mHttpClient = new VolleyClient(context);

        Looper tempLooper = looper == null ? Looper.myLooper() : looper;
        mHandler = new ResponderHandler<T>(this, tempLooper);

        mGsonModel = gson;

        mListener = listener;
    }

    /*
     * QueryBuilder qb = userDao.queryBuilder();
     * qb.where(Properties.FirstName.eq("Joe"),
     * qb.or(Properties.YearOfBirth.gt(1970),
     * qb.and(Properties.YearOfBirth.eq(1970),
     * Properties.MonthOfBirth.ge(10)))); List youngJoes = qb.list();
     * 
     * 
     * Select * from xxx where xxxx Order by xxxx offset xxx limit xxx
     */
    public void query(Context context, VolleyClientRequest api,
            int offset, int limit, WhereCondition cond,
            WhereCondition... condMore) {
        final QueryBuilder<T> queryBuilder = mDao.queryBuilder();
        queryBuilder.offset(offset);
        queryBuilder.limit(limit);
        queryBuilder.where(cond, condMore);
        
        List<T> entities = queryBuilder.list();
        if (entities != null && !entities.isEmpty()) {
            mHandler.obtainMessage(
                    QUERY_SUCCESS_MESSAGE,
                    new Object[] { ModelProviderListener.RESULT_STATUS_OK,
                            ModelProviderListener.FROM_DATABASE, entities, null })
                    .sendToTarget();
            mHandler.obtainMessage(QUERY_FINISH_MESSAGE).sendToTarget();
            return;
        } else {
            if (!HttpUtils.isNetworkAvaliable(mContext)) {
                mHandler.obtainMessage(
                        QUERY_FAILURE_MESSAGE,
                        new Object[] {
                                ModelProviderListener.RESULT_STATUS_NETOWRK_NOT_AVALIABLE,
                                ModelProviderListener.FROM_SERVER, null, null })
                        .sendToTarget();
                mHandler.obtainMessage(QUERY_FINISH_MESSAGE)
                        .sendToTarget();
                return;
            }
            api.setResponseHandler(new JSONResponseListener() {

                @Override
                public void onResponse(BasicJSONResponse response) {
                    if (BasicJSONResponse.SUCCESS == response.getErrorCode()) {
                        List<T> entities = mGsonModel.parseObjects(response
                                .getJSONObject());
                        mDao.insertInTx(entities);
                        entities = queryBuilder.list();
                        mHandler.obtainMessage(
                                QUERY_SUCCESS_MESSAGE,
                                new Object[] {
                                        ModelProviderListener.RESULT_STATUS_OK,
                                        ModelProviderListener.FROM_SERVER,
                                        entities, response }).sendToTarget();
                        mHandler.obtainMessage(QUERY_FINISH_MESSAGE)
                                .sendToTarget();
                    } else {
                        mHandler.obtainMessage(
                                QUERY_FAILURE_MESSAGE,
                                new Object[] {
                                        ModelProviderListener.RESULT_STATUS_API_ERROR,
                                        ModelProviderListener.FROM_SERVER,
                                        null, response }).sendToTarget();
                        mHandler.obtainMessage(QUERY_FINISH_MESSAGE)
                                .sendToTarget();
                    }
                }

            });
            mHttpClient.get(mContext, api);
        }
    }

    public void update(Context context, VolleyClientRequest api,
            int offset, int limit, WhereCondition cond,
            WhereCondition... condMore) {
        final QueryBuilder<T> queryBuilder = mDao.queryBuilder();
        queryBuilder.offset(offset);
        queryBuilder.limit(limit);
        queryBuilder.where(cond, condMore);
        
        if (!HttpUtils.isNetworkAvaliable(mContext)) {
            mHandler.obtainMessage(
                    UPDATE_FAILURE_MESSAGE,
                    new Object[] {
                            ModelProviderListener.RESULT_STATUS_NETOWRK_NOT_AVALIABLE,
                            ModelProviderListener.FROM_SERVER, null, null })
                    .sendToTarget();
            mHandler.obtainMessage(UPDATE_FINISH_MESSAGE)
                    .sendToTarget();
            return;
        }
        api.setResponseHandler(new JSONResponseListener() {

            @Override
            public void onResponse(BasicJSONResponse response) {
                if (BasicJSONResponse.SUCCESS == response.getErrorCode()) {
                    List<T> entities = mGsonModel.parseObjects(response
                            .getJSONObject());
                    mDao.deleteAll();
                    mDao.insertInTx(entities);
                    entities = queryBuilder.list();
                    mHandler.obtainMessage(
                            UPDATE_SUCCESS_MESSAGE,
                            new Object[] {
                                    ModelProviderListener.RESULT_STATUS_OK,
                                    ModelProviderListener.FROM_SERVER,
                                    entities, response }).sendToTarget();
                    mHandler.obtainMessage(UPDATE_FINISH_MESSAGE)
                            .sendToTarget();
                } else {
                    mHandler.obtainMessage(
                            UPDATE_FAILURE_MESSAGE,
                            new Object[] {
                                    ModelProviderListener.RESULT_STATUS_API_ERROR,
                                    ModelProviderListener.FROM_SERVER, null,
                                    response }).sendToTarget();
                    mHandler.obtainMessage(UPDATE_FINISH_MESSAGE)
                            .sendToTarget();
                }
            }

        });
        mHttpClient.get(mContext, api);
    }

    public AbstractDao<T, Long> getDao() {
        return mDao;
    }

    @SuppressWarnings("unchecked")
    public void handleMessage(Message msg) {
        Object[] response;

        if (mListener != null) {
            switch (msg.what) {
            case QUERY_START_MESSAGE:
                mListener.onQueryStart();
                break;
            case QUERY_FINISH_MESSAGE:
                // do noting
                break;
            case QUERY_SUCCESS_MESSAGE:
            case QUERY_FAILURE_MESSAGE:
                response = (Object[]) msg.obj;
                if (response != null && response.length >= 3) {

                    mListener.onQueryFinish((Integer) response[0],
                            (Integer) response[1], (List<T>) response[2],
                            (BasicJSONResponse) response[3]);

                }
                break;
            case UPDATE_START_MESSAGE:
                mListener.onUpdateStart();
                break;
            case UPDATE_FINISH_MESSAGE:
                // do noting
                break;
            case UPDATE_SUCCESS_MESSAGE:
            case UPDATE_FAILURE_MESSAGE:
                response = (Object[]) msg.obj;
                if (response != null && response.length >= 3) {

                    mListener.onUpdateFinish((Integer) response[0],
                            (Integer) response[1], (List<T>) response[2],
                            (BasicJSONResponse) response[3]);

                }
                break;
            case QUERY_CANCEL_MESSAGE:
            case UPDATE_CANCEL_MESSAGE:
                mListener.onCancel();
                break;
            }
        }
    }

    /**
     * Avoid leaks by using a non-anonymous handler class.
     */
    private static class ResponderHandler<T> extends Handler {
        private final VolleyModelProvider<T> mProvider;

        public ResponderHandler(VolleyModelProvider<T> provider, Looper looper) {
            super(looper);
            mProvider = provider;
        }

        @Override
        public void handleMessage(Message msg) {
            mProvider.handleMessage(msg);
        }
    }
}
