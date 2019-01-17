package com.genonbeta.TrebleShot.dialog;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.FileListAdapter;
import com.genonbeta.TrebleShot.object.FileBookmarkObject;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.android.framework.io.DocumentFile;

import java.util.ArrayList;
import java.util.List;

/**
 * created by: Veli
 * date: 26.02.2018 08:53
 */

public class FileRenameDialog<T extends FileListAdapter.GenericFileHolder> extends AbstractSingleTextInputDialog
{
    public static final String TAG = FileRenameDialog.class.getSimpleName();
    public static final int JOB_RENAME_FILES = 0;

    private List<T> mItemList = new ArrayList<>();

    public FileRenameDialog(Context context, List<T> itemList, final OnFileRenameListener renameListener)
    {
        super(context);

        mItemList.addAll(itemList);

        setTitle(mItemList.size() > 1
                ? R.string.text_renameMultipleItems
                : R.string.text_rename);

        getEditText().setText(mItemList.size() > 1
                ? "%d"
                : mItemList.get(0).fileName);

        setOnProceedClickListener(R.string.butn_rename, new OnProceedClickListener()
        {
            @Override
            public boolean onProceedClick(AlertDialog dialog)
            {
                final String renameTo = getEditText().getText().toString();

                if (getItemList().size() == 1
                        && renameFile(getItemList().get(0), renameTo, renameListener)) {
                    if (renameListener != null)
                        renameListener.onFileRenameCompleted(getContext());
                    return true;
                }

                try {
                    String.format(renameTo, getItemList().size());
                } catch (Exception e) {
                    return false;
                }

                return WorkerService.run(getContext(), new WorkerService.RunningTask(TAG, JOB_RENAME_FILES)
                {
                    @Override
                    protected void onRun()
                    {
                        publishStatusText(getService().getString(R.string.text_renameMultipleItems));

                        int fileId = 0;

                        for (T fileHolder : getItemList()) {
                            String ext = FileUtils.getFileFormat(fileHolder.file.getName());
                            ext = ext != null ? String.format(".%s", ext) : "";

                            renameFile(fileHolder, String.format("%s%s", String.format(renameTo, fileId), ext), renameListener);
                            fileId++;
                        }

                        if (renameListener != null)
                            renameListener.onFileRenameCompleted(getService());
                    }
                });
            }
        });
    }

    public List<T> getItemList()
    {
        return mItemList;
    }

    public boolean renameFile(T holder, String renameTo, OnFileRenameListener renameListener)
    {
        try {
            if (holder instanceof FileListAdapter.BookmarkedDirectoryHolder) {
                FileBookmarkObject object = ((FileListAdapter.BookmarkedDirectoryHolder) holder).getBookmarkObject();

                if (object != null) {
                    object.title = renameTo;
                    AppUtils.getDatabase(getContext()).publish(object);
                }
            } else if (holder.file.canWrite() && holder.file.renameTo(renameTo)) {
                if (renameListener != null)
                    renameListener.onFileRename(holder.file, renameTo);

                return true;
            }
        } catch (Exception e) {
        }

        return false;
    }

    public interface OnFileRenameListener
    {
        void onFileRename(DocumentFile file, String displayName);

        void onFileRenameCompleted(Context context);
    }
}
