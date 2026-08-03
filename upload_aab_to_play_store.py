import os
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

PACKAGE_NAME = 'com.gpsspy.gpstracker'
KEY_FILE = r'C:\Users\b\Wealth-Manager\play-console-key.json'
AAB_PATH = r'app\build\outputs\bundle\release\app-release.aab'

def main():
    if not os.path.exists(KEY_FILE):
        print(f"Key file {KEY_FILE} not found!")
        return
        
    if not os.path.exists(AAB_PATH):
        print(f"AAB file {AAB_PATH} not found!")
        return

    credentials = service_account.Credentials.from_service_account_file(
        KEY_FILE, scopes=['https://www.googleapis.com/auth/androidpublisher']
    )
    service = build('androidpublisher', 'v3', credentials=credentials)
    
    try:
        print("Creating edit...")
        edit_request = service.edits().insert(body={}, packageName=PACKAGE_NAME)
        edit = edit_request.execute()
        edit_id = edit['id']
        
        print(f"Uploading AAB: {AAB_PATH} ...")
        media = MediaFileUpload(AAB_PATH, mimetype='application/octet-stream')
        bundle_response = service.edits().bundles().upload(
            packageName=PACKAGE_NAME,
            editId=edit_id,
            media_body=media
        ).execute()
        
        version_code = bundle_response['versionCode']
        print(f"Successfully uploaded AAB with version code: {version_code}")
        
        print(f"Assigning AAB to internal track...")
        track_body = {
            'releases': [{
                'name': f"1.2.69-local", # Just a local run name
                'versionCodes': [str(version_code)],
                'status': 'completed'
            }]
        }
        
        service.edits().tracks().update(
            packageName=PACKAGE_NAME,
            editId=edit_id,
            track='internal',
            body=track_body
        ).execute()
        
        print("Committing edit...")
        service.edits().commit(packageName=PACKAGE_NAME, editId=edit_id, changesNotSentForReview=True).execute()
        print("Local AAB upload and track assignment completed successfully!")
        
    except Exception as e:
        print(f"Error occurred: {e}")

if __name__ == '__main__':
    main()
