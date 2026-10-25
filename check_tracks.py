import os
from google.oauth2 import service_account
from googleapiclient.discovery import build

PACKAGE_NAME = 'com.gpsspy.gpstracker'
KEY_FILE = 'C:\\Users\\b\\Wealth-Manager\\play-console-key.json'

def main():
    if not os.path.exists(KEY_FILE):
        print(f"Key file {KEY_FILE} not found!")
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
        
        print("\nFetching tracks...")
        tracks_response = service.edits().tracks().list(
            packageName=PACKAGE_NAME, editId=edit_id
        ).execute()
        
        tracks = tracks_response.get('tracks', [])
        for track in tracks:
            print(f"Track: {track.get('track')}")
            for release in track.get('releases', []):
                print(f"  Release Name: {release.get('name')}")
                print(f"  Version Codes: {release.get('versionCodes')}")
                
        service.edits().delete(packageName=PACKAGE_NAME, editId=edit_id).execute()
    except Exception as e:
        print(f"Error occurred: {e}")

if __name__ == '__main__':
    main()
