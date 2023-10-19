package com.izzdarki.wallet.ui.credentials

// It's hard to test image selection in an instrumented test, because it involves files and in reality also the file selection by the user
// Manual testing is maybe easier
// Check: After manual test, exactly the new images should be in files directory, no more and no less

// Adding new images
// 1. Create new entry / Open existing entry
// 2. Add front image
// 3. Add back image / or don't
// 4. Save / Cancel

// Deleting images
// 1. Open existing entry with 1 / or 2 images
// 2. Delete front image / Delete back image
// 3. Delete other image too / or don't
// 4. Save / Cancel

// Changing images
// 1. Open existing entry
// 2. Change front image / Change back image
// 3. Change other image too / or don't
// 4. Save / Cancel

// Errors
// Pick too big image file

// Special Images
// 1. Open Example card image / Open Mahler card / Create new card and use mahler easter egg
// Test delete and change as above

// Delete complete card and check if all images are deleted