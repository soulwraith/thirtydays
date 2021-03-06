package com.challenge.bennho.a30days.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import com.challenge.bennho.a30days.R;
import com.challenge.bennho.a30days.fragments.FragmentPhotoItem;
import com.challenge.bennho.a30days.helpers.AndroidUtils;
import com.challenge.bennho.a30days.helpers.OverlayBuilder;
import com.challenge.bennho.a30days.helpers.UserPhotoHelpers;
import com.challenge.bennho.a30days.models.User;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PhotoActivity extends MyActivity {

    private final int PICK_PHOTO_CODE = 2345;
    private final int TAKE_PHOTO_CODE = 2346;
    private final int TAKE_PHOTO_PERMISSION_CODE = 2347;
    private User user;
    private int dayPlan;
    private ViewPager pagerPhoto;
    private PhotoAdapter photoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        onLayoutSet();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        user = new User(this);
        user.reload();

        dayPlan = 1;
        if (getIntent() != null) {
            dayPlan = getIntent().getIntExtra("dayPlan", 1);
        }

        pagerPhoto = (ViewPager) findViewById(R.id.pagerPhoto);
        photoAdapter = new PhotoAdapter(getSupportFragmentManager());
        pagerPhoto.setAdapter(photoAdapter);

        onDayChanged(dayPlan);
        refreshPhoto();


        pagerPhoto.setCurrentItem(dayPlan - 1);
        setListeners();

        checkPermission();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.photo_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.camera) {
            takePhoto();
            return true;
        } else if (id == R.id.gallery) {
            pickImageFromGallery();
            return true;
        } else if (id == R.id.delete) {
            deleteImage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == TAKE_PHOTO_PERMISSION_CODE) {
            for(int result: grantResults){
                if (result != PackageManager.PERMISSION_GRANTED) {
                    finish();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TAKE_PHOTO_CODE && resultCode == RESULT_OK) {
            photoSuccessTaken();
        } else if (requestCode == PICK_PHOTO_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                try {
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(selectedImage,
                            filePathColumn, null, null, null);
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String picturePath = cursor.getString(columnIndex);
                    cursor.close();
                    imageSuccessPickedFromGallery(picturePath);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void pickImageFromGallery() {
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, PICK_PHOTO_CODE);
    }

    public void imageSuccessPickedFromGallery(String path) {
        photoSelected(new File(path));
    }


    public void takePhoto() {

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, getOutputUri());
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (intent.resolveActivity(getApplicationContext().getPackageManager()) != null) {
            startActivityForResult(intent, TAKE_PHOTO_CODE);
        }
    }

    public void photoSuccessTaken() {
        photoSelected(new File(getOutputUri().getPath()));
    }

    private void photoSelected(File photoFile) {
        File newfile = UserPhotoHelpers.getDayPhotoImageFilePath(this, dayPlan, user.getCurrentIteration());
        File thumbFile = UserPhotoHelpers.getDayPhotoThumbnailFilePath(this, dayPlan, user.getCurrentIteration());

        try {
            AndroidUtils.moveFileToPrivateDir(this, photoFile, newfile.getName());
            AndroidUtils.moveFileToPrivateDir(this, photoFile, thumbFile.getName());

            Bitmap image = handleSamplingAndRotationBitmap(this, Uri.fromFile(newfile), 1024, 1024);
            FileOutputStream out = null;
            out = new FileOutputStream(newfile.getAbsoluteFile());
            image.compress(Bitmap.CompressFormat.JPEG, 100, out);

            Bitmap image2 = handleSamplingAndRotationBitmap(this, Uri.fromFile(thumbFile), 200, 200);
            FileOutputStream out2 = null;
            out2 = new FileOutputStream(thumbFile.getAbsoluteFile());
            image2.compress(Bitmap.CompressFormat.JPEG, 100, out2);

        } catch (IOException e) {
            e.printStackTrace();
        }

        refreshPhoto();
    }

    public void refreshPhoto() {
        photoAdapter.notifyDataSetChanged();
    }


    private Uri getOutputUri() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File output = new File(dir, "days30photo.jpg");
        return Uri.fromFile(output);
    }

    private void deleteImage() {
        OverlayBuilder.build(this)
                .setOverlayType(OverlayBuilder.OverlayType.OkCancel)
                .setTitle(getString(R.string.avty_photo_confirm_delete_title))
                .setContent(String.format(getString(R.string.avty_photo_confirm_delete_content),
                        String.valueOf(dayPlan)))
                .setRunnables(new Runnable() {
                    @Override
                    public void run() {
                        UserPhotoHelpers.getDayPhotoImageFilePath(PhotoActivity.this, dayPlan, user.getCurrentIteration()).delete();
                        UserPhotoHelpers.getDayPhotoThumbnailFilePath(PhotoActivity.this, dayPlan, user.getCurrentIteration()).delete();
                        refreshPhoto();
                    }
                })
                .show();
    }

    private void onDayChanged(int day) {
        dayPlan = day;
        setTitle(String.format(getString(R.string.avty_photo_title), String.valueOf(dayPlan)));
    }

    private void setListeners() {
        pagerPhoto.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                onDayChanged(position + 1);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }


    public class PhotoAdapter extends FragmentPagerAdapter {
        public PhotoAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            FragmentPhotoItem fragment = new FragmentPhotoItem();
            Bundle args = new Bundle();

            args.putInt("dayPlan", i + 1);
            args.putInt("currentIteration", user.getCurrentIteration());

            fragment.setArguments(args);
            fragment.setPhotoItemListener(new FragmentPhotoItem.PhotoItemListener() {
                @Override
                public void requestTakePhoto() {
                    takePhoto();
                }
            });
            return fragment;
        }

        @Override
        public int getCount() {
            return 30;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

    }


    /**
     * This method is responsible for solving the rotation issue if exist. Also scale the images to
     * 1024x1024 resolution
     *
     * @param context       The current context
     * @param selectedImage The Image URI
     * @return Bitmap image results
     * @throws IOException
     */
    public static Bitmap handleSamplingAndRotationBitmap(Context context, Uri selectedImage, int MAX_HEIGHT,
                                                         int MAX_WIDTH)
            throws IOException {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream imageStream = context.getContentResolver().openInputStream(selectedImage);
        BitmapFactory.decodeStream(imageStream, null, options);
        imageStream.close();

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        imageStream = context.getContentResolver().openInputStream(selectedImage);
        Bitmap img = BitmapFactory.decodeStream(imageStream, null, options);

        img = rotateImageIfRequired(img, selectedImage);
        return img;
    }

    /**
     * Calculate an inSampleSize for use in a {@link BitmapFactory.Options} object when decoding
     * bitmaps using the decode* methods from {@link BitmapFactory}. This implementation calculates
     * the closest inSampleSize that will result in the final decoded bitmap having a width and
     * height equal to or larger than the requested width and height. This implementation does not
     * ensure a power of 2 is returned for inSampleSize which can be faster when decoding but
     * results in a larger bitmap which isn't as useful for caching purposes.
     *
     * @param options   An options object with out* params already populated (run through a decode*
     *                  method with inJustDecodeBounds==true
     * @param reqWidth  The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    private static int calculateInSampleSize(BitmapFactory.Options options,
                                             int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee a final image
            // with both dimensions larger than or equal to the requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger inSampleSize).

            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down further
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }

    /**
     * Rotate an image if required.
     *
     * @param img           The image bitmap
     * @param selectedImage Image URI
     * @return The resulted Bitmap after manipulation
     */
    private static Bitmap rotateImageIfRequired(Bitmap img, Uri selectedImage) throws IOException {

        ExifInterface ei = new ExifInterface(selectedImage.getPath());
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }


    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        TAKE_PHOTO_PERMISSION_CODE);
            }
        }
    }


}
